package org.lml.thirdService.pay.entity.req;

import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.pay.constant.PayChannel;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
/**
 * 模拟支付渠道回调参数。
 */
public class MockPayNotifyRequest {
    /**
     * TCC事务号，回调时用于定位当前充值支付事务。
     */
    @NotBlank(message = "transactionId is blank")
    private String transactionId;

    /**
     * 系统内部支付单号。
     */
    @NotBlank(message = "payOrderId is blank")
    private String payOrderId;

    /**
     * 模拟渠道生成的交易流水号。
     */
    @NotBlank(message = "channelTradeNo is blank")
    private String channelTradeNo;

    /**
     * 回调所属支付渠道。
     */
    @NotNull(message = "payChannel is null")
    private PayChannel payChannel;

    /**
     * 模拟支付结果，true表示支付成功，false表示支付失败。
     */
    private boolean success = true;
}
