package org.lml.business.billing.facade;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.lml.billing.api.BillingFacadeService;
import org.lml.billing.enums.BillingOperationStatus;
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
import org.lml.business.billing.domain.account.service.BillingAccountService;
import org.lml.business.billing.domain.compensation.service.BillingCompensationService;
import org.lml.business.billing.domain.flow.entity.BillingFlow;
import org.lml.business.billing.domain.flow.service.BillingFlowService;
import org.lml.business.billing.domain.tcc.service.BillingTccService;
import org.lml.business.billing.exception.BillingBizException;
import org.lml.business.billing.support.BillingAccountSnapshot;
import org.lml.business.billing.support.BillingLockTemplate;
import org.lml.common.result.CommonResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 积分账务 Dubbo 门面实现。
 * 统一对外暴露余额查询、充值、扣减和补偿能力。
 */
@Slf4j
@DubboService(version = "1.0.0")
public class BillingFacadeServiceImpl implements BillingFacadeService {

    @Resource
    private BillingAccountService billingAccountService;

    @Resource
    private BillingFlowService billingFlowService;

    @Resource
    private BillingTccService billingTccService;

    @Resource
    private BillingCompensationService billingCompensationService;

    @Resource
    private BillingLockTemplate billingLockTemplate;

    @Override
    public CommonResult<BillingBalanceResp> queryBalance(BillingBalanceQueryReq request) {
        try {
            return CommonResult.successResponse(billingAccountService.queryBalance(request.getUserId()), "查询积分余额成功");
        } catch (BillingBizException ex) {
            return CommonResult.errorResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("查询积分余额失败，userId={}", request.getUserId(), ex);
            return CommonResult.errorResponse("查询积分余额失败");
        }
    }

    /**
     * 充值入口需要处理请求号幂等和账户级并发。
     *
     * @param request 充值请求
     * @return 充值结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<BillingRechargeResp> recharge(BillingRechargeReq request) {
        try {
            BillingRechargeResp response = billingLockTemplate.executeWithUserLock(request.getUserId(), () -> doRecharge(request));
            return CommonResult.successResponse(response, "积分充值成功");
        } catch (BillingBizException ex) {
            return CommonResult.errorResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("积分充值失败，requestNo={}, userId={}", request.getRequestNo(), request.getUserId(), ex);
            return CommonResult.errorResponse("积分充值失败");
        }
    }

    @Override
    public CommonResult<BillingFlowPageResp> queryFlow(BillingFlowQueryReq request) {
        try {
            return CommonResult.successResponse(billingFlowService.queryFlow(request), "查询积分流水成功");
        } catch (BillingBizException ex) {
            return CommonResult.errorResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("查询积分流水失败，userId={}", request.getUserId(), ex);
            return CommonResult.errorResponse("查询积分流水失败");
        }
    }

    @Override
    public CommonResult<BillingTccResp> tryDebit(BillingTryDebitReq request) {
        try {
            return CommonResult.successResponse(billingTccService.tryDebit(request), "积分预扣减成功");
        } catch (BillingBizException ex) {
            return CommonResult.errorResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("积分预扣减失败，tccTransactionNo={}", request.getTccTransactionNo(), ex);
            return CommonResult.errorResponse("积分预扣减失败");
        }
    }

    @Override
    public CommonResult<BillingTccResp> confirmDebit(BillingConfirmDebitReq request) {
        try {
            return CommonResult.successResponse(billingTccService.confirmDebit(request), "积分确认扣减成功");
        } catch (BillingBizException ex) {
            return CommonResult.errorResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("积分确认扣减失败，tccTransactionNo={}", request.getTccTransactionNo(), ex);
            return CommonResult.errorResponse("积分确认扣减失败");
        }
    }

    @Override
    public CommonResult<BillingTccResp> cancelDebit(BillingCancelDebitReq request) {
        try {
            return CommonResult.successResponse(billingTccService.cancelDebit(request), "积分取消扣减成功");
        } catch (BillingBizException ex) {
            return CommonResult.errorResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("积分取消扣减失败，tccTransactionNo={}", request.getTccTransactionNo(), ex);
            return CommonResult.errorResponse("积分取消扣减失败");
        }
    }

    @Override
    public CommonResult<String> triggerCompensation(BillingCompensationReq request) {
        try {
            if (!StringUtils.hasText(request.getBizNo()) && !StringUtils.hasText(request.getTccTransactionNo())) {
                return CommonResult.errorResponse("bizNo 和 tccTransactionNo 至少需要传一个");
            }
            return CommonResult.successResponse(billingCompensationService.triggerCompensation(request), "补偿执行成功");
        } catch (BillingBizException ex) {
            return CommonResult.errorResponse(ex.getMessage());
        } catch (Exception ex) {
            log.error("触发积分补偿失败，requestNo={}", request.getRequestNo(), ex);
            return CommonResult.errorResponse("补偿执行失败");
        }
    }

    private BillingRechargeResp doRecharge(BillingRechargeReq request) {
        //获取积分流水信息
        BillingFlow existed = billingFlowService.findByRequestNo(request.getRequestNo());
        if (existed != null) {
            return buildRechargeResp(existed);
        }
        //执行积分充值
        BillingAccountSnapshot snapshot = billingAccountService.increaseBalance(
                request.getUserId(), BigDecimal.valueOf(request.getAmount().longValue()));
        //保存充值流水
        BillingFlow flow = billingFlowService.saveRechargeFlow(request, snapshot);
        //构建返回参数
        return buildRechargeResp(flow);
    }

    private BillingRechargeResp buildRechargeResp(BillingFlow flow) {
        BillingRechargeResp response = new BillingRechargeResp();
        response.setRequestNo(flow.getRequestNo());
        response.setBizNo(flow.getBizNo());
        response.setUserId(flow.getUserId());
        response.setAmount(flow.getAmount().intValue());
        response.setBalance(flow.getBalanceAfter());
        response.setFrozenBalance(flow.getFrozenAfter());
        response.setStatus(BillingOperationStatus.SUCCESS);
        return response;
    }
}
