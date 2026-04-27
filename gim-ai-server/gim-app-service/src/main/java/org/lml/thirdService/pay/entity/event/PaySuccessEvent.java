package org.lml.thirdService.pay.entity.event;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import org.lml.thirdService.pay.constant.PayChannel;

/**
 * 支付成功领域事件
 * * @author Hollis
 */
@Getter
@Setter
public class PaySuccessEvent {

    /**
     * 支付订单号
     */
    private String payOrderId;

    /**
     * 支付成功时间
     */
    private Date paySucceedTime;

    /**
     * 渠道流水号
     */
    private String channelStreamId;

    /**
     * 支付渠道
     */
    private PayChannel payChannel;

    /**
     * 支付金额
     */
    private BigDecimal paidAmount;

    /**
     * 支付操作人
     */
    private String payer;
}
