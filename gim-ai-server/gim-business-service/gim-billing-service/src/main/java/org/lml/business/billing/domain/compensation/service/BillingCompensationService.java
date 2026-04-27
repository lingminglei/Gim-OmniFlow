package org.lml.business.billing.domain.compensation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lml.billing.enums.BillingChangeType;
import org.lml.billing.req.BillingCompensationReq;
import org.lml.business.billing.domain.flow.service.BillingFlowService;
import org.lml.business.billing.domain.tcc.entity.BillingTccRecord;
import org.lml.business.billing.domain.tcc.mapper.BillingTccRecordMapper;
import org.lml.business.billing.domain.tcc.service.BillingTccService;
import org.lml.business.billing.exception.BillingBizException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 账务补偿服务。
 * 这里只保留最小必要能力：
 * 1. 扫描超时预扣减并自动回退。
 * 2. 修复已完成事务缺失的关键流水。
 * 不再额外维护补偿任务表，避免把 billing 做成独立调度系统。
 */
@Slf4j
@Service
public class BillingCompensationService {

    @Resource
    private BillingTccRecordMapper billingTccRecordMapper;

    @Resource
    private BillingTccService billingTccService;

    @Resource
    private BillingFlowService billingFlowService;

    /**
     * 人工触发单笔补偿。
     *
     * @param request 补偿请求
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public String triggerCompensation(BillingCompensationReq request) {
        BillingTccRecord record = resolveRecord(request);
        processRecord(record, request.getRequestNo(), defaultRemark(request.getRemark(), "人工触发补偿"));
        return "补偿执行完成";
    }

    /**
     * 定时扫描超时的预扣减事务，并直接回退。
     */
    @Scheduled(fixedDelayString = "${billing.compensation.scan-interval-ms:60000}")
    public void scanExpiredTryingRecords() {
        List<BillingTccRecord> records = billingTccRecordMapper.selectList(new LambdaQueryWrapper<BillingTccRecord>()
                .eq(BillingTccRecord::getStatus, "TRYING")
                .le(BillingTccRecord::getExpireTime, new Date()));
        for (BillingTccRecord record : records) {
            try {
                String requestNo = "AUTO-CANCEL-" + record.getTccTransactionNo();
                processRecord(record, requestNo, "系统扫描到超时预扣减，自动回退");
            } catch (Exception ex) {
                log.error("自动回退超时预扣减失败，tccTransactionNo={}", record.getTccTransactionNo(), ex);
            }
        }
    }

    /**
     * 定时修复已确认但缺失确认流水的事务。
     */
    @Scheduled(fixedDelayString = "${billing.compensation.scan-interval-ms:60000}")
    public void scanConfirmedWithoutFlow() {
        List<BillingTccRecord> records = billingTccRecordMapper.selectList(new LambdaQueryWrapper<BillingTccRecord>()
                .eq(BillingTccRecord::getStatus, "CONFIRMED"));
        for (BillingTccRecord record : records) {
            if (billingFlowService.findByTccTransactionNoAndChangeType(record.getTccTransactionNo(), BillingChangeType.CONFIRM_DEBIT) != null) {
                continue;
            }
            try {
                String requestNo = "AUTO-CONFIRM-FLOW-" + record.getTccTransactionNo();
                processRecord(record, requestNo, "系统补记确认扣减流水");
            } catch (Exception ex) {
                log.error("自动补记确认流水失败，tccTransactionNo={}", record.getTccTransactionNo(), ex);
            }
        }
    }

    /**
     * 定时修复已取消但缺失取消流水的事务。
     */
    @Scheduled(fixedDelayString = "${billing.compensation.scan-interval-ms:60000}")
    public void scanCanceledWithoutFlow() {
        List<BillingTccRecord> records = billingTccRecordMapper.selectList(new LambdaQueryWrapper<BillingTccRecord>()
                .eq(BillingTccRecord::getStatus, "CANCELED"));
        for (BillingTccRecord record : records) {
            if (billingFlowService.findByTccTransactionNoAndChangeType(record.getTccTransactionNo(), BillingChangeType.CANCEL_DEBIT) != null) {
                continue;
            }
            try {
                String requestNo = "AUTO-CANCEL-FLOW-" + record.getTccTransactionNo();
                processRecord(record, requestNo, "系统补记取消扣减流水");
            } catch (Exception ex) {
                log.error("自动补记取消流水失败，tccTransactionNo={}", record.getTccTransactionNo(), ex);
            }
        }
    }

    private void processRecord(BillingTccRecord record, String requestNo, String remark) {
        if ("TRYING".equals(record.getStatus())) {
            billingTccService.cancelExpiredTrying(record, requestNo, remark);
            return;
        }
        if ("CONFIRMED".equals(record.getStatus())) {
            billingTccService.repairConfirmedFlow(record, requestNo, remark);
            return;
        }
        if ("CANCELED".equals(record.getStatus())) {
            billingTccService.repairCanceledFlow(record, requestNo, remark);
            return;
        }
        throw new BillingBizException("不支持的补偿状态: " + record.getStatus());
    }

    private BillingTccRecord resolveRecord(BillingCompensationReq request) {
        if (!StringUtils.hasText(request.getBizNo()) && !StringUtils.hasText(request.getTccTransactionNo())) {
            throw new BillingBizException("bizNo 和 tccTransactionNo 至少需要传一个");
        }
        if (StringUtils.hasText(request.getTccTransactionNo())) {
            BillingTccRecord record = billingTccService.getByTransactionNo(request.getTccTransactionNo());
            if (record == null) {
                throw new BillingBizException("未找到对应的预扣减事务记录");
            }
            return record;
        }
        List<BillingTccRecord> records = billingTccRecordMapper.selectList(new LambdaQueryWrapper<BillingTccRecord>()
                .eq(BillingTccRecord::getBizNo, request.getBizNo())
                .orderByDesc(BillingTccRecord::getCreateTime));
        if (records.isEmpty()) {
            throw new BillingBizException("未找到对应业务单号的预扣减事务记录");
        }
        return records.get(0);
    }

    private String defaultRemark(String remark, String defaultRemark) {
        return StringUtils.hasText(remark) ? remark : defaultRemark;
    }
}
