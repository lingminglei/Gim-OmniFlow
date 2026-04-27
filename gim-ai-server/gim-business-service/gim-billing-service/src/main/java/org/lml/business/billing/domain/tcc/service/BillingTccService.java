package org.lml.business.billing.domain.tcc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lml.billing.enums.BillingChangeType;
import org.lml.billing.enums.BillingOperationStatus;
import org.lml.billing.enums.BillingTccStatus;
import org.lml.billing.req.BillingCancelDebitReq;
import org.lml.billing.req.BillingConfirmDebitReq;
import org.lml.billing.req.BillingTryDebitReq;
import org.lml.billing.resp.BillingBalanceResp;
import org.lml.billing.resp.BillingTccResp;
import org.lml.business.billing.domain.account.service.BillingAccountService;
import org.lml.business.billing.domain.flow.entity.BillingFlow;
import org.lml.business.billing.domain.flow.service.BillingFlowService;
import org.lml.business.billing.domain.tcc.entity.BillingTccRecord;
import org.lml.business.billing.domain.tcc.mapper.BillingTccRecordMapper;
import org.lml.business.billing.exception.BillingBizException;
import org.lml.business.billing.support.BillingAccountSnapshot;
import org.lml.business.billing.support.BillingLockTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 积分扣减 TCC 服务。
 * 负责 Try/Confirm/Cancel 状态推进、幂等以及补偿修复。
 */
@Slf4j
@Service
public class BillingTccService {

    @Resource
    private BillingTccRecordMapper billingTccRecordMapper;

    @Resource
    private BillingAccountService billingAccountService;

    @Resource
    private BillingFlowService billingFlowService;

    @Resource
    private BillingLockTemplate billingLockTemplate;

    /**
     * 执行 Try 预扣减。
     *
     * @param request 预扣减请求
     * @return TCC 响应
     */
    @Transactional(rollbackFor = Exception.class)
    public BillingTccResp tryDebit(BillingTryDebitReq request) {
        return billingLockTemplate.executeWithUserLock(request.getUserId(), () -> doTryDebit(request));
    }

    /**
     * 执行 Confirm 正式扣减。
     *
     * @param request 确认扣减请求
     * @return TCC 响应
     */
    @Transactional(rollbackFor = Exception.class)
    public BillingTccResp confirmDebit(BillingConfirmDebitReq request) {
        return billingLockTemplate.executeWithUserLock(request.getUserId(), () -> doConfirmDebit(request));
    }

    /**
     * 执行 Cancel 回退释放。
     *
     * @param request 取消扣减请求
     * @return TCC 响应
     */
    @Transactional(rollbackFor = Exception.class)
    public BillingTccResp cancelDebit(BillingCancelDebitReq request) {
        return billingLockTemplate.executeWithUserLock(request.getUserId(), () -> doCancelDebit(request));
    }

    /**
     * 对超时 TRYING 事务执行系统自动回滚。
     *
     * @param record TCC 记录
     * @param requestNo 自动补偿请求号
     * @param remark 备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredTrying(BillingTccRecord record, String requestNo, String remark) {
        BillingCancelDebitReq request = new BillingCancelDebitReq();
        request.setRequestNo(requestNo);
        request.setTccTransactionNo(record.getTccTransactionNo());
        request.setUserId(record.getUserId());
        request.setBizNo(record.getBizNo());
        request.setSourceSystem("billing-compensation");
        request.setAmount(record.getAmount().intValue());
        request.setRemark(remark);
        cancelDebit(request);
    }

    /**
     * 修复已确认但缺失确认流水的事务。
     *
     * @param record TCC 记录
     * @param requestNo 补偿请求号
     * @param remark 补偿备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void repairConfirmedFlow(BillingTccRecord record, String requestNo, String remark) {
        billingLockTemplate.executeWithUserLock(record.getUserId(), () -> {
            if (billingFlowService.findByTccTransactionNoAndChangeType(record.getTccTransactionNo(), BillingChangeType.CONFIRM_DEBIT) != null) {
                return null;
            }
            BillingFlow tryFlow = requireTryFlow(record.getTccTransactionNo());
            BillingAccountSnapshot snapshot = BillingAccountSnapshot.builder()
                    .userId(record.getUserId())
                    .balanceBefore(tryFlow.getBalanceAfter())
                    .balanceAfter(tryFlow.getBalanceAfter())
                    .frozenBefore(tryFlow.getFrozenAfter())
                    .frozenAfter(tryFlow.getFrozenAfter().subtract(record.getAmount()))
                    .build();
            billingFlowService.saveConfirmFlow(record, requestNo, "billing-compensation", remark, snapshot);
            return null;
        });
    }

    /**
     * 修复已取消但缺失取消流水的事务。
     *
     * @param record TCC 记录
     * @param requestNo 补偿请求号
     * @param remark 补偿备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void repairCanceledFlow(BillingTccRecord record, String requestNo, String remark) {
        billingLockTemplate.executeWithUserLock(record.getUserId(), () -> {
            if (billingFlowService.findByTccTransactionNoAndChangeType(record.getTccTransactionNo(), BillingChangeType.CANCEL_DEBIT) != null) {
                return null;
            }
            BillingFlow tryFlow = requireTryFlow(record.getTccTransactionNo());
            BillingAccountSnapshot snapshot = BillingAccountSnapshot.builder()
                    .userId(record.getUserId())
                    .balanceBefore(tryFlow.getBalanceAfter())
                    .balanceAfter(tryFlow.getBalanceAfter().add(record.getAmount()))
                    .frozenBefore(tryFlow.getFrozenAfter())
                    .frozenAfter(tryFlow.getFrozenAfter().subtract(record.getAmount()))
                    .build();
            billingFlowService.saveCancelFlow(record, requestNo, "billing-compensation", remark, snapshot);
            return null;
        });
    }

    /**
     * 按事务号查询 TCC 记录。
     *
     * @param tccTransactionNo 事务号
     * @return TCC 记录
     */
    public BillingTccRecord getByTransactionNo(String tccTransactionNo) {
        return billingTccRecordMapper.selectOne(new LambdaQueryWrapper<BillingTccRecord>()
                .eq(BillingTccRecord::getTccTransactionNo, tccTransactionNo)
                .last("limit 1"));
    }

    private BillingTccResp doTryDebit(BillingTryDebitReq request) {
        //先查询TCC 事务记录
        BillingTccRecord record = getByTransactionNo(request.getTccTransactionNo());
        if (record != null) {
            validateRecordMatch(record, request.getUserId(), request.getBizNo(), request.getAmount());
            return buildCurrentResp(request.getRequestNo(), record, BillingOperationStatus.SUCCESS);
        }

        //TCC Try 阶段预扣减积分。
        BillingAccountSnapshot snapshot = billingAccountService.tryFreeze(
                request.getUserId(), BigDecimal.valueOf(request.getAmount().longValue()));

        //新建记录TCC事务记录操作
        BillingTccRecord newRecord = new BillingTccRecord();
        newRecord.setTccTransactionNo(request.getTccTransactionNo());
        newRecord.setUserId(request.getUserId());
        newRecord.setBizNo(request.getBizNo());
        newRecord.setRequestNo(request.getRequestNo());
        newRecord.setAmount(BigDecimal.valueOf(request.getAmount().longValue()));
        newRecord.setStatus(BillingTccStatus.TRYING.name());
        newRecord.setExpireTime(new Date(System.currentTimeMillis() + request.getExpireSeconds() * 1000L));
        newRecord.setSourceSystem(request.getSourceSystem());
        newRecord.setRemark(request.getRemark());
        newRecord.setRetryCount(0);
        billingTccRecordMapper.insert(newRecord);

        //记录积分预扣处理流水
        billingFlowService.saveTryDebitFlow(request, snapshot);
        return buildResp(request.getRequestNo(), newRecord, snapshot, BillingOperationStatus.SUCCESS);
    }

    private BillingTccResp doConfirmDebit(BillingConfirmDebitReq request) {
        BillingTccRecord record = requireRecord(request.getTccTransactionNo());
        validateRecordMatch(record, request.getUserId(), request.getBizNo(), request.getAmount());

        if (BillingTccStatus.CONFIRMED.name().equals(record.getStatus())) {
            return buildCurrentResp(request.getRequestNo(), record, BillingOperationStatus.SUCCESS);
        }
        if (BillingTccStatus.CANCELED.name().equals(record.getStatus())) {
            throw new BillingBizException("TCC 事务已取消，不能再确认扣减");
        }

        BillingAccountSnapshot snapshot = billingAccountService.confirmDebit(
                request.getUserId(), BigDecimal.valueOf(request.getAmount().longValue()));
        if (billingTccRecordMapper.confirmFromTrying(record.getId(), request.getRequestNo()) <= 0) {
            throw new BillingBizException("TCC 确认状态推进失败");
        }
        record.setStatus(BillingTccStatus.CONFIRMED.name());
        record.setConfirmRequestNo(request.getRequestNo());
        billingFlowService.saveConfirmFlow(record, request.getRequestNo(), request.getSourceSystem(), request.getRemark(), snapshot);
        return buildResp(request.getRequestNo(), record, snapshot, BillingOperationStatus.SUCCESS);
    }

    private BillingTccResp doCancelDebit(BillingCancelDebitReq request) {
        BillingTccRecord record = getByTransactionNo(request.getTccTransactionNo());
        if (record == null) {
            BillingTccRecord emptyCancel = new BillingTccRecord();
            emptyCancel.setTccTransactionNo(request.getTccTransactionNo());
            emptyCancel.setUserId(request.getUserId());
            emptyCancel.setBizNo(request.getBizNo());
            emptyCancel.setRequestNo(request.getRequestNo());
            emptyCancel.setCancelRequestNo(request.getRequestNo());
            emptyCancel.setAmount(BigDecimal.valueOf(request.getAmount().longValue()));
            emptyCancel.setStatus(BillingTccStatus.CANCELED.name());
            emptyCancel.setExpireTime(new Date());
            emptyCancel.setSourceSystem(request.getSourceSystem());
            emptyCancel.setRemark(defaultRemark(request.getRemark(), "空回滚"));
            emptyCancel.setRetryCount(0);
            billingTccRecordMapper.insert(emptyCancel);
            return buildCurrentResp(request.getRequestNo(), emptyCancel, BillingOperationStatus.SUCCESS);
        }

        validateRecordMatch(record, request.getUserId(), request.getBizNo(), request.getAmount());
        if (BillingTccStatus.CANCELED.name().equals(record.getStatus())) {
            return buildCurrentResp(request.getRequestNo(), record, BillingOperationStatus.SUCCESS);
        }
        if (BillingTccStatus.CONFIRMED.name().equals(record.getStatus())) {
            throw new BillingBizException("TCC 事务已确认，不能再取消");
        }

        BillingAccountSnapshot snapshot = billingAccountService.cancelDebit(
                request.getUserId(), BigDecimal.valueOf(request.getAmount().longValue()));
        if (billingTccRecordMapper.cancelFromTrying(record.getId(), request.getRequestNo(), request.getRemark()) <= 0) {
            throw new BillingBizException("TCC 取消状态推进失败");
        }
        record.setStatus(BillingTccStatus.CANCELED.name());
        record.setCancelRequestNo(request.getRequestNo());
        billingFlowService.saveCancelFlow(record, request.getRequestNo(), request.getSourceSystem(), request.getRemark(), snapshot);
        return buildResp(request.getRequestNo(), record, snapshot, BillingOperationStatus.SUCCESS);
    }

    private BillingTccRecord requireRecord(String tccTransactionNo) {
        BillingTccRecord record = getByTransactionNo(tccTransactionNo);
        if (record == null) {
            throw new BillingBizException("未找到对应的 TCC 事务记录");
        }
        return record;
    }

    private BillingFlow requireTryFlow(String tccTransactionNo) {
        BillingFlow flow = billingFlowService.findByTccTransactionNoAndChangeType(tccTransactionNo, BillingChangeType.TRY_DEBIT);
        if (flow == null) {
            throw new BillingBizException("缺少 Try 阶段流水，无法执行补偿修复");
        }
        return flow;
    }

    private void validateRecordMatch(BillingTccRecord record, String userId, String bizNo, Integer amount) {
        if (!record.getUserId().equals(userId)) {
            throw new BillingBizException("TCC 事务对应用户不一致");
        }
        if (!record.getBizNo().equals(bizNo)) {
            throw new BillingBizException("TCC 事务对应业务单号不一致");
        }
        if (record.getAmount().compareTo(BigDecimal.valueOf(amount.longValue())) != 0) {
            throw new BillingBizException("TCC 事务对应积分数量不一致");
        }
    }

    private BillingTccResp buildCurrentResp(String requestNo, BillingTccRecord record, BillingOperationStatus status) {
        BillingBalanceResp balance = billingAccountService.queryBalance(record.getUserId());
        BillingTccResp response = new BillingTccResp();
        response.setRequestNo(requestNo);
        response.setTccTransactionNo(record.getTccTransactionNo());
        response.setBizNo(record.getBizNo());
        response.setUserId(record.getUserId());
        response.setAmount(record.getAmount().intValue());
        response.setTccStatus(BillingTccStatus.valueOf(record.getStatus()));
        response.setStatus(status);
        response.setBalance(balance.getBalance());
        response.setFrozenBalance(balance.getFrozenBalance());
        return response;
    }

    private BillingTccResp buildResp(String requestNo,
                                     BillingTccRecord record,
                                     BillingAccountSnapshot snapshot,
                                     BillingOperationStatus status) {
        BillingTccResp response = new BillingTccResp();
        response.setRequestNo(requestNo);
        response.setTccTransactionNo(record.getTccTransactionNo());
        response.setBizNo(record.getBizNo());
        response.setUserId(record.getUserId());
        response.setAmount(record.getAmount().intValue());
        response.setTccStatus(BillingTccStatus.valueOf(record.getStatus()));
        response.setStatus(status);
        response.setBalance(snapshot.getBalanceAfter());
        response.setFrozenBalance(snapshot.getFrozenAfter());
        return response;
    }

    private String defaultRemark(String remark, String defaultRemark) {
        return StringUtils.hasText(remark) ? remark : defaultRemark;
    }
}
