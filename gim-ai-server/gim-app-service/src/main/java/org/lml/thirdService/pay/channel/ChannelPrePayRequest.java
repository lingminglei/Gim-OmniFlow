package org.lml.thirdService.pay.channel;

import lombok.Builder;
import lombok.Getter;
import org.lml.thirdService.pay.constant.PayChannel;

import java.math.BigDecimal;

@Getter
@Builder
/**
 * 统一的渠道预下单请求。
 */
public class ChannelPrePayRequest {
    /**
     * 支付单号
     */
    private String payOrderId;
    /**
     * 金额
     * 单位：元
     */
    private BigDecimal amount;

    /**
     * 订单描述
     */
    private String description;

    /**
     * 附加信息
     */
    private String attach;
}
