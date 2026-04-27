package org.lml.thirdService.pay.entity.event;

import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.pay.constant.PayChannel;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 退款成功领域事件
 * <p>
 * 当支付渠道（微信/支付宝等）完成退款扣款，或内部资产退款（如积分撤销）完成后触发。
 * 业务订阅方需根据此事件：
 * 1. 回收对应的积分/Token/网盘额度。
 * 2. 如果余额不足，需进入“欠费/锁定”状态或触发人工审核流程。
 * </p>
 * @author Hollis
 */
@Getter
@Setter
public class RefundSuccessEvent {

    /**
     * 支付单号
     */
    private String payOrderId;

    /**
     * 退款单号
     */
    private String refundOrderId;

    /**
     * 退款成功时间
     */
    private Date refundedTime;

    /**
     * 渠道流水号
     */
    private String channelStreamId;

    /**
     * 退款金额
     */
    private BigDecimal refundedAmount;

    /**
     * 退款渠道
     */
    private PayChannel refundChannel;
}
