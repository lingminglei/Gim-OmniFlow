package org.lml.business.billing.domain.flow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lml.billing.enums.BillingChangeType;
import org.lml.billing.req.BillingFlowQueryReq;
import org.lml.billing.req.BillingRechargeReq;
import org.lml.billing.req.BillingTryDebitReq;
import org.lml.billing.resp.BillingFlowPageResp;
import org.lml.billing.resp.BillingFlowResp;
import org.lml.business.billing.domain.flow.entity.BillingFlow;
import org.lml.business.billing.domain.flow.mapper.BillingFlowMapper;
import org.lml.business.billing.domain.tcc.entity.BillingTccRecord;
import org.lml.business.billing.exception.BillingBizException;
import org.lml.business.billing.support.BillingAccountSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 积分流水服务。
 * 负责幂等查询、流水落库以及分页查询。
 */
@Slf4j
@Service
public class BillingFlowService {

    @Resource
    private BillingFlowMapper billingFlowMapper;

    /**
     * 按请求号查询流水，用于充值等接口幂等。
     *
     * @param requestNo 请求号
     * @return 流水记录
     */
    public BillingFlow findByRequestNo(String requestNo) {
        if (!StringUtils.hasText(requestNo)) {
            return null;
        }
        return billingFlowMapper.selectOne(new LambdaQueryWrapper<BillingFlow>()
                .eq(BillingFlow::getRequestNo, requestNo)
                .last("limit 1"));
    }

    /**
     * 查询指定 TCC 事务和流水类型的记录。
     *
     * @param tccTransactionNo TCC 事务号
     * @param changeType 流水类型
     * @return 流水记录
     */
    public BillingFlow findByTccTransactionNoAndChangeType(String tccTransactionNo, BillingChangeType changeType) {
        if (!StringUtils.hasText(tccTransactionNo) || changeType == null) {
            return null;
        }
        return billingFlowMapper.selectOne(new LambdaQueryWrapper<BillingFlow>()
                .eq(BillingFlow::getTccTransactionNo, tccTransactionNo)
                .eq(BillingFlow::getChangeType, changeType.name())
                .last("limit 1"));
    }

    /**
     * 保存充值流水。
     *
     * @param request 充值请求
     * @param snapshot 账户快照
     * @return 流水记录
     */
    public BillingFlow saveRechargeFlow(BillingRechargeReq request, BillingAccountSnapshot snapshot) {
        BillingFlow flow = buildBaseFlow(request.getUserId(), request.getRequestNo(), request.getBizNo(),
                request.getSourceSystem(), BillingChangeType.RECHARGE, snapshot);
        flow.setAmount(BigDecimal.valueOf(request.getAmount().longValue()));
        flow.setRemark(defaultRemark(request.getRemark(), "积分充值"));
        return saveFlow(flow);
    }

    /**
     * 保留积分预扣流水
     *
     * @param request Try 请求
     * @param snapshot 账户快照
     * @return 流水记录
     */
    public BillingFlow saveTryDebitFlow(BillingTryDebitReq request, BillingAccountSnapshot snapshot) {
        BillingFlow flow = buildBaseFlow(request.getUserId(), request.getRequestNo(), request.getBizNo(),
                request.getSourceSystem(), BillingChangeType.TRY_DEBIT, snapshot);
        flow.setAmount(BigDecimal.valueOf(request.getAmount().longValue()));
        flow.setTransactionId(request.getTccTransactionNo());
        flow.setTccTransactionNo(request.getTccTransactionNo());
        flow.setRemark(defaultRemark(request.getRemark(), "积分预扣减"));
        return saveFlow(flow);
    }

    /**
     * 保存 TCC Confirm 阶段流水。
     */
    public BillingFlow saveConfirmFlow(BillingTccRecord record,
                                       String requestNo,
                                       String sourceSystem,
                                       String remark,
                                       BillingAccountSnapshot snapshot) {
        BillingFlow flow = buildBaseFlow(record.getUserId(), requestNo, record.getBizNo(),
                sourceSystem, BillingChangeType.CONFIRM_DEBIT, snapshot);
        flow.setAmount(record.getAmount());
        flow.setTransactionId(record.getTccTransactionNo());
        flow.setTccTransactionNo(record.getTccTransactionNo());
        flow.setRemark(defaultRemark(remark, "积分确认扣减"));
        return saveFlow(flow);
    }

    /**
     * 保存 TCC Cancel 阶段流水。
     */
    public BillingFlow saveCancelFlow(BillingTccRecord record,
                                      String requestNo,
                                      String sourceSystem,
                                      String remark,
                                      BillingAccountSnapshot snapshot) {
        BillingFlow flow = buildBaseFlow(record.getUserId(), requestNo, record.getBizNo(),
                sourceSystem, BillingChangeType.CANCEL_DEBIT, snapshot);
        flow.setAmount(record.getAmount());
        flow.setTransactionId(record.getTccTransactionNo());
        flow.setTccTransactionNo(record.getTccTransactionNo());
        flow.setRemark(defaultRemark(remark, "积分取消扣减"));
        return saveFlow(flow);
    }

    /**
     * 保存补偿流水。
     */
    public BillingFlow saveCompensationFlow(BillingTccRecord record,
                                            String requestNo,
                                            String sourceSystem,
                                            String remark,
                                            BillingAccountSnapshot snapshot) {
        BillingFlow flow = buildBaseFlow(record.getUserId(), requestNo, record.getBizNo(),
                sourceSystem, BillingChangeType.COMPENSATION, snapshot);
        flow.setAmount(record.getAmount());
        flow.setTransactionId(record.getTccTransactionNo());
        flow.setTccTransactionNo(record.getTccTransactionNo());
        flow.setRemark(defaultRemark(remark, "积分补偿流水"));
        return saveFlow(flow);
    }

    /**
     * 分页查询积分流水。
     *
     * @param request 查询请求
     * @return 分页结果
     */
    public BillingFlowPageResp queryFlow(BillingFlowQueryReq request) {
        LambdaQueryWrapper<BillingFlow> wrapper = new LambdaQueryWrapper<BillingFlow>()
                .eq(BillingFlow::getUserId, request.getUserId())
                .orderByDesc(BillingFlow::getCreateTime);

        if (StringUtils.hasText(request.getBizNo())) {
            wrapper.eq(BillingFlow::getBizNo, request.getBizNo());
        }
        if (StringUtils.hasText(request.getRequestNo())) {
            wrapper.eq(BillingFlow::getRequestNo, request.getRequestNo());
        }
        if (StringUtils.hasText(request.getTccTransactionNo())) {
            wrapper.eq(BillingFlow::getTccTransactionNo, request.getTccTransactionNo());
        }
        if (StringUtils.hasText(request.getSourceSystem())) {
            wrapper.eq(BillingFlow::getSourceSystem, request.getSourceSystem());
        }
        if (request.getChangeType() != null) {
            wrapper.eq(BillingFlow::getChangeType, request.getChangeType().name());
        }

        Page<BillingFlow> page = new Page<>(request.getPageNo(), request.getPageSize());
        Page<BillingFlow> result = billingFlowMapper.selectPage(page, wrapper);

        BillingFlowPageResp response = new BillingFlowPageResp();
        response.setPageNo(request.getPageNo());
        response.setPageSize(request.getPageSize());
        response.setTotal(result.getTotal());

        List<BillingFlowResp> records = new ArrayList<>();
        for (BillingFlow flow : result.getRecords()) {
            records.add(toResp(flow));
        }
        response.setRecords(records);
        return response;
    }

    private BillingFlow saveFlow(BillingFlow flow) {
        BillingFlow existed = findByRequestNo(flow.getRequestNo());
        if (existed != null) {
            return existed;
        }
        flow.setDataHash(buildDataHash(flow));
        billingFlowMapper.insert(flow);
        return flow;
    }

    private BillingFlow buildBaseFlow(String userId,
                                      String requestNo,
                                      String bizNo,
                                      String sourceSystem,
                                      BillingChangeType changeType,
                                      BillingAccountSnapshot snapshot) {
        BillingFlow flow = new BillingFlow();
        flow.setUserId(userId);
        flow.setRequestNo(requestNo);
        flow.setBizNo(bizNo);
        flow.setSourceSystem(sourceSystem);
        flow.setBizType(changeType.name());
        flow.setChangeType(changeType.name());
        flow.setBalanceBefore(snapshot.getBalanceBefore());
        flow.setBalanceAfter(snapshot.getBalanceAfter());
        flow.setFrozenBefore(snapshot.getFrozenBefore());
        flow.setFrozenAfter(snapshot.getFrozenAfter());
        return flow;
    }

    private BillingFlowResp toResp(BillingFlow flow) {
        BillingFlowResp response = new BillingFlowResp();
        response.setId(flow.getId());
        response.setUserId(flow.getUserId());
        response.setRequestNo(flow.getRequestNo());
        response.setBizNo(flow.getBizNo());
        response.setSourceSystem(flow.getSourceSystem());
        response.setTccTransactionNo(flow.getTccTransactionNo());
        response.setAmount(flow.getAmount());
        response.setBalanceBefore(flow.getBalanceBefore());
        response.setBalanceAfter(flow.getBalanceAfter());
        response.setFrozenBefore(flow.getFrozenBefore());
        response.setFrozenAfter(flow.getFrozenAfter());
        response.setRemark(flow.getRemark());
        response.setCreateTime(flow.getCreateTime());
        if (StringUtils.hasText(flow.getChangeType())) {
            response.setChangeType(BillingChangeType.valueOf(flow.getChangeType()));
        }
        return response;
    }

    /**
     * 对关键字段生成摘要，防止流水记录被静默篡改。
     *
     * @param flow 流水记录
     * @return 摘要值
     */
    private String buildDataHash(BillingFlow flow) {
        String raw = new StringJoiner("|")
                .add(defaultString(flow.getUserId()))
                .add(defaultString(flow.getRequestNo()))
                .add(defaultString(flow.getBizNo()))
                .add(defaultString(flow.getSourceSystem()))
                .add(defaultString(flow.getBizType()))
                .add(defaultString(flow.getChangeType()))
                .add(flow.getAmount() == null ? "" : flow.getAmount().toPlainString())
                .add(flow.getBalanceBefore() == null ? "" : flow.getBalanceBefore().toPlainString())
                .add(flow.getBalanceAfter() == null ? "" : flow.getBalanceAfter().toPlainString())
                .add(flow.getFrozenBefore() == null ? "" : flow.getFrozenBefore().toPlainString())
                .add(flow.getFrozenAfter() == null ? "" : flow.getFrozenAfter().toPlainString())
                .add(defaultString(flow.getTccTransactionNo()))
                .add(defaultString(flow.getRemark()))
                .toString();
        return md5(raw);
    }

    private String defaultRemark(String remark, String defaultRemark) {
        return StringUtils.hasText(remark) ? remark : defaultRemark;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String md5(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BillingBizException("生成流水摘要失败");
        }
    }
}
