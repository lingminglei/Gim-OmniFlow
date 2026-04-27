package org.lml.controller.pay;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.result.CommonResult;
import org.lml.service.pay.RechargePaymentService;
import org.lml.thirdService.pay.entity.dto.PayOrder;
import org.lml.thirdService.pay.entity.req.MockPayNotifyRequest;
import org.lml.thirdService.pay.entity.req.RechargeCancelRequest;
import org.lml.thirdService.pay.entity.req.RechargeConfirmRequest;
import org.lml.thirdService.pay.entity.req.RechargeTryPayRequest;
import org.lml.thirdService.pay.entity.resp.RechargeTryPayResponse;
import org.lml.utils.RedisUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import java.util.Objects;

import static org.lml.common.constant.RedisCacheConstant.PAY_TOKEN_INFO;
import static org.lml.common.constant.RedisCacheConstant.PAY_TOKEN_INFO_TIME;

@RestController
@RequestMapping("/pay/recharge")
@Slf4j
/**
 * 充值支付控制器。
 * 对外提供建单、发起支付、取消支付、回调入口和支付单查询接口。
 */
public class RechargePayController {
    @Resource
    private RechargePaymentService rechargePaymentService;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 执行充值支付尝试阶段。
     * 前端选择套餐后，先调用这个接口创建订单、支付单和事务号。
     *
     * @param request 尝试支付请求
     * @return 建单结果
     */
    @PostMapping("/tryPay")
    public CommonResult<RechargeTryPayResponse> tryPay(@Validated @RequestBody RechargeTryPayRequest request) {
        log.info("收到充值支付尝试请求，套餐编码={}, 幂等号={}, 支付渠道={}",
                request.getPackageCode(), request.getIdentifier(), request.getPayChannel());

        String tokenKey = PAY_TOKEN_INFO + StpUtil.getLoginId();

        String payToken = (String) redisUtils.get(tokenKey);

        if(Objects.isNull(payToken) || !request.getIdentifier().equals(payToken)){
            //校验 identifier 合法性，确保支付接口的安全性
            return CommonResult.errorResponse("避免重复下单，请刷新页面后重试！");
        }

        RechargeTryPayResponse response = rechargePaymentService.tryRecharge(request);
        log.info("充值支付尝试请求处理完成，支付单号={}, 事务号={}, 交易单号={}",
                response.getPayOrderId());
        return CommonResult.successResponse(response);
    }

    /**
     * 执行充值支付确认阶段。
     * 前端拿着已生成的订单发起实际支付，这里只推进到待回调状态。
     *
     * @param request 确认支付请求
     * @return 发起支付后的结果
     */
    @PostMapping("/confirm")
    public CommonResult<RechargeTryPayResponse> confirm(@Validated @RequestBody RechargeConfirmRequest request) {
        log.info("收到充值支付确认请求，事务号={}, 支付单号={}, 支付渠道={}", request.getPayOrderId(), request.getPayChannel());
        RechargeTryPayResponse response = rechargePaymentService.confirm(request);
        log.info("充值支付确认请求处理完成，事务号={}, 支付单号={}, 当前状态={}", request.getPayOrderId());
        return CommonResult.successResponse(response);
    }

    /**
     * 发起充值支付取消。
     * 这里只负责发送取消事件，真正的终态回滚由MQ消费端完成。
     *
     * @param request 取消请求
     * @return 成功响应
     */
    @PostMapping("/cancel")
    public CommonResult<Void> cancel(@Validated @RequestBody RechargeCancelRequest request) {
        log.info("收到充值支付取消请求，事务号={}, 支付单号={}, 取消原因={}",
                request.getTransactionId(), request.getPayOrderId(), request.getReason());
        rechargePaymentService.cancelRecharge(request);
        log.info("充值支付取消请求处理完成，事务号={}, 支付单号={}",
                request.getTransactionId(), request.getPayOrderId());
        return CommonResult.successResponse();
    }

    /**
     * 接收模拟支付回调。
     * 回调接口只负责投递终态消息，不直接改订单终态。
     *
     * @param request 模拟回调参数
     * @return 成功响应
     */
    @PostMapping("/callback/mock")
    public CommonResult<Void> mockCallback(@Validated @RequestBody MockPayNotifyRequest request) {
        log.info("收到模拟支付回调，事务号={}, 支付单号={}, 是否成功={}, 支付渠道={}",
                request.getTransactionId(), request.getPayOrderId(), request.isSuccess(), request.getPayChannel());
        rechargePaymentService.onMockNotify(request);
        log.info("模拟支付回调处理完成，事务号={}, 支付单号={}, 是否成功={}",
                request.getTransactionId(), request.getPayOrderId(), request.isSuccess());
        return CommonResult.successResponse();
    }

    /**
     * 查询支付单当前状态。
     *
     * @param payOrderId 支付单号
     * @return 支付单详情
     */
    @GetMapping("/order/{payOrderId}")
    public CommonResult<PayOrder> query(@PathVariable String payOrderId) {
        log.info("收到充值支付单查询请求，支付单号={}", payOrderId);
        PayOrder payOrder = rechargePaymentService.queryPayOrder(payOrderId);
        log.info("充值支付单查询完成，支付单号={}, 当前状态={}", payOrderId, payOrder.getOrderState());
        return CommonResult.successResponse(payOrder);
    }
}
