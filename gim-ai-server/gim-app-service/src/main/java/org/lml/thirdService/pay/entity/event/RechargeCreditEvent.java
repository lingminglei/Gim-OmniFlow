package org.lml.thirdService.pay.entity.event;

import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.pay.constant.PayChannel;

import java.math.BigDecimal;

@Getter
@Setter
/**
 * 充值支付成功回调事件。
 * 由支付回调投递到MQ，消费端再推进订单终态并完成积分到账。
 */
public class RechargeCreditEvent {

    /**
     * 支付单号。
     */
    private String payOrderId;

    /**
     * 套餐编码。
     */
    private String packageCode;

    /**
     * 到账积分数量。
     */
    private Integer creditAmount;

    /**
     * 本次支付金额。
     */
    private BigDecimal orderAmount;

    /**
     * 支付渠道。
     */
    private PayChannel channel;

    /**
     * 渠道交易流水号。
     */
    private String channelTradeNo;
}
