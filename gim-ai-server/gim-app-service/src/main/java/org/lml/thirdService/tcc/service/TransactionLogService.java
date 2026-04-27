package org.lml.thirdService.tcc.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.lml.thirdService.tcc.entity.TransActionLogState;
import org.lml.thirdService.tcc.entity.TransCancelSuccessType;
import org.lml.thirdService.tcc.entity.TransConfirmSuccessType;
import org.lml.thirdService.tcc.entity.TransTrySuccessType;
import org.lml.thirdService.tcc.entity.TransactionLog;
import org.lml.thirdService.tcc.mapper.TransactionLogMapper;
import org.lml.thirdService.tcc.request.TccRequest;
import org.lml.thirdService.tcc.response.TransactionCancelResponse;
import org.lml.thirdService.tcc.response.TransactionConfirmResponse;
import org.lml.thirdService.tcc.response.TransactionTryResponse;

/**
 * TCC事务日志服务。
 * 负责Try、Confirm、Cancel三个阶段的日志记录与状态流转。
 *
 * @author Hollis
 */
@Slf4j
public class TransactionLogService extends ServiceImpl<TransactionLogMapper, TransactionLog> {

    /**
     * 执行TCC事务的Try阶段。
     * 如果事务日志不存在则创建TRY记录，存在则按幂等直接返回成功。
     *
     * 1、这里要先查一把，如果在 try 的过程中，发现存在操作日志，说明发现了空回滚问题。
     *
     * 2、如果日志不存在，则插入一条操作日志即可。
     *
     * @param tccRequest TCC事务请求
     * @return Try阶段执行结果
     */
    public TransactionTryResponse tryTransaction(TccRequest tccRequest) {
        log.info("TCC try started, transactionId={}, businessScene={}, businessModule={}",
                tccRequest.getTransactionId(), tccRequest.getBusinessScene(), tccRequest.getBusinessModule());
        TransactionLog existTransactionLog = getExistTransLog(tccRequest);
        if (existTransactionLog == null) {
            TransactionLog transactionLog = new TransactionLog(tccRequest, TransActionLogState.TRY);
            if (this.save(transactionLog)) {
                log.info("TCC try saved new transaction log, transactionId={}", tccRequest.getTransactionId());
                return new TransactionTryResponse(true, TransTrySuccessType.TRY_SUCCESS);
            }
            log.error("TCC try failed while saving transaction log, transactionId={}", tccRequest.getTransactionId());
            return new TransactionTryResponse(false, "TRY_FAILED", "TRY_FAILED");
        }

        log.info("TCC try reused existing transaction log, transactionId={}, currentState={}",
                tccRequest.getTransactionId(), existTransactionLog.getState());
        return new TransactionTryResponse(true, TransTrySuccessType.DUPLICATED_TRY);
    }

    /**
     * 执行TCC事务的Confirm阶段。
     * 只允许TRY状态推进到CONFIRM，已CONFIRM视为幂等成功。
     *
     * 在confirm 中，只处理 try 过来的情况
     *
     * @param tccRequest TCC事务请求
     * @return Confirm阶段执行结果
     */
    public TransactionConfirmResponse confirmTransaction(TccRequest tccRequest) {
        log.info("TCC confirm started, transactionId={}, businessScene={}, businessModule={}",
                tccRequest.getTransactionId(), tccRequest.getBusinessScene(), tccRequest.getBusinessModule());
        TransactionLog existTransactionLog = getExistTransLog(tccRequest);
        if (existTransactionLog == null) {
            log.error("TCC confirm could not find transaction log, transactionId={}, businessScene={}, businessModule={}",
                    tccRequest.getTransactionId(), tccRequest.getBusinessScene(), tccRequest.getBusinessModule());
            throw new UnsupportedOperationException("transacton can not confirm");
        }

        if (existTransactionLog.getState() == TransActionLogState.TRY) {
            baseMapper.normalizeDeleted(existTransactionLog.getId());
            if (baseMapper.confirmFromTry(existTransactionLog.getId()) > 0) {
                log.info("TCC confirm succeeded, transactionId={}, logId={}",
                        tccRequest.getTransactionId(), existTransactionLog.getId());
                return new TransactionConfirmResponse(true, TransConfirmSuccessType.CONFIRM_SUCCESS);
            }

            log.error("TCC confirm failed during state transition, transactionId={}, logId={}, currentState={}",
                    tccRequest.getTransactionId(), existTransactionLog.getId(), existTransactionLog.getState());
            return new TransactionConfirmResponse(false, "CONFIRM_FAILED", "CONFIRM_FAILED");
        }

        if (existTransactionLog.getState() == TransActionLogState.CONFIRM) {
            log.info("TCC confirm duplicated, transactionId={}, logId={}",
                    tccRequest.getTransactionId(), existTransactionLog.getId());
            return new TransactionConfirmResponse(true, TransConfirmSuccessType.DUPLICATED_CONFIRM);
        }

        log.error("TCC confirm rejected because transaction is in invalid state, transactionId={}, state={}",
                tccRequest.getTransactionId(), existTransactionLog.getState());
        throw new UnsupportedOperationException("transacton can not confirm :" + existTransactionLog.getState());
    }

    /**
     * 执行TCC事务的Cancel阶段。
     * 支持空回滚和TRY后的取消，已CANCEL视为幂等成功。
     *
     * 在Cancel 阶段，可以接受处理： try - cancel ; confirm -cancel; cencel- cancel; 阶段；
     *
     * @param tccRequest TCC事务请求
     * @return Cancel阶段执行结果
     */
    public TransactionCancelResponse cancelTransaction(TccRequest tccRequest) {
        log.info("TCC cancel started, transactionId={}, businessScene={}, businessModule={}",
                tccRequest.getTransactionId(), tccRequest.getBusinessScene(), tccRequest.getBusinessModule());
        TransactionLog existTransactionLog = getExistTransLog(tccRequest);
        if (existTransactionLog == null) {
            // 说明这里发生了空回滚的情况，需要插入一条日志记录；
            TransactionLog transactionLog = new TransactionLog(tccRequest, TransActionLogState.CANCEL, TransCancelSuccessType.EMPTY_CANCEL);
            if (this.save(transactionLog)) {
                log.info("TCC cancel recorded empty cancel, transactionId={}", tccRequest.getTransactionId());
                return new TransactionCancelResponse(true, TransCancelSuccessType.EMPTY_CANCEL);
            }
            log.error("TCC cancel failed while recording empty cancel, transactionId={}", tccRequest.getTransactionId());
            return new TransactionCancelResponse(false, "EMPTY_CANCEL_FAILED", "EMPTY_CANCEL_FAILED");
        }

        if (existTransactionLog.getState() == TransActionLogState.TRY) {
            baseMapper.normalizeDeleted(existTransactionLog.getId());
            if (baseMapper.cancelFromTry(existTransactionLog.getId(), TransCancelSuccessType.CANCEL_AFTER_TRY_SUCCESS.name()) > 0) {
                log.info("TCC cancel succeeded after try, transactionId={}, logId={}",
                        tccRequest.getTransactionId(), existTransactionLog.getId());
                return new TransactionCancelResponse(true, TransCancelSuccessType.CANCEL_AFTER_TRY_SUCCESS);
            }
            log.error("TCC cancel failed during state transition from TRY, transactionId={}, logId={}",
                    tccRequest.getTransactionId(), existTransactionLog.getId());
            return new TransactionCancelResponse(false, "CANCEL_FAILED", "CANCEL_FAILED");
        }

        if (existTransactionLog.getState() == TransActionLogState.CONFIRM) {
            log.error("TCC cancel rejected because transaction already confirmed, transactionId={}, logId={}",
                    tccRequest.getTransactionId(), existTransactionLog.getId());
            throw new UnsupportedOperationException("transacton can not cancel after confirm");
        }

        if (existTransactionLog.getState() == TransActionLogState.CANCEL) {
            log.info("TCC cancel duplicated, transactionId={}, logId={}",
                    tccRequest.getTransactionId(), existTransactionLog.getId());
            return new TransactionCancelResponse(true, TransCancelSuccessType.DUPLICATED_CANCEL);
        }

        log.error("TCC cancel failed because transaction is in invalid state, transactionId={}, state={}",
                tccRequest.getTransactionId(), existTransactionLog.getState());
        return new TransactionCancelResponse(false, "CANCEL_FAILED", "CANCEL_FAILED");
    }

    /**
     * 按事务维度查询事务日志，兼容历史deleted为空的数据。
     *
     * @param request TCC事务请求
     * @return 事务日志
     */
    private TransactionLog getExistTransLog(TccRequest request) {
        TransactionLog transactionLog = baseMapper.selectLatestByBiz(
                request.getTransactionId(),
                request.getBusinessScene(),
                request.getBusinessModule());
        if (transactionLog == null) {
            log.warn("TCC transaction log not found, transactionId={}, businessScene={}, businessModule={}",
                    request.getTransactionId(), request.getBusinessScene(), request.getBusinessModule());
            return null;
        }
        if (transactionLog.getDeleted() == null) {
            log.warn("TCC transaction log found with null deleted flag, transactionId={}, logId={}",
                    request.getTransactionId(), transactionLog.getId());
        }
        return transactionLog;
    }
}
