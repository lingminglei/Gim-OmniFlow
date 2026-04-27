package org.lml.billing.api;

import org.lml.billing.req.BillingBalanceQueryReq;
import org.lml.billing.req.BillingCancelDebitReq;
import org.lml.billing.req.BillingCompensationReq;
import org.lml.billing.req.BillingConfirmDebitReq;
import org.lml.billing.req.BillingFlowQueryReq;
import org.lml.billing.req.BillingRechargeReq;
import org.lml.billing.req.BillingTryDebitReq;
import org.lml.billing.resp.BillingBalanceResp;
import org.lml.billing.resp.BillingFlowPageResp;
import org.lml.billing.resp.BillingRechargeResp;
import org.lml.billing.resp.BillingTccResp;
import org.lml.common.result.CommonResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 积分账务 Dubbo 门面服务。
 * 对外统一提供积分查询、充值、扣减以及补偿能力。
 */
public interface BillingFacadeService {

    /**
     * 查询指定用户的积分余额与冻结积分。
     *
     * @param request 查询请求
     * @return 余额查询结果
     */
    CommonResult<BillingBalanceResp> queryBalance(@RequestBody @Validated BillingBalanceQueryReq request);

    /**
     * 执行积分充值。
     *
     * @param request 充值请求
     * @return 充值结果
     */
    CommonResult<BillingRechargeResp> recharge(@RequestBody @Validated BillingRechargeReq request);

    /**
     * 查询积分流水明细。
     *
     * @param request 流水查询请求
     * @return 分页流水结果
     */
    CommonResult<BillingFlowPageResp> queryFlow(@RequestBody @Validated BillingFlowQueryReq request);

    /**
     * TCC Try 阶段，执行积分预扣减。
     *
     * @param request 预扣减请求
     * @return TCC 结果
     */
    CommonResult<BillingTccResp> tryDebit(@RequestBody @Validated BillingTryDebitReq request);

    /**
     * TCC Confirm 阶段，执行积分正式扣减。
     *
     * @param request 确认扣减请求
     * @return TCC 结果
     */
    CommonResult<BillingTccResp> confirmDebit(@RequestBody @Validated BillingConfirmDebitReq request);

    /**
     * TCC Cancel 阶段，执行积分释放回退。
     *
     * @param request 取消扣减请求
     * @return TCC 结果
     */
    CommonResult<BillingTccResp> cancelDebit(@RequestBody @Validated BillingCancelDebitReq request);

    /**
     * 触发指定业务单号或事务号的补偿。
     *
     * @param request 补偿请求
     * @return 补偿执行结果
     */
    CommonResult<String> triggerCompensation(@RequestBody @Validated BillingCompensationReq request);
}
