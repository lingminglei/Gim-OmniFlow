package org.lml.thirdService.pay.channel;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;


/**
 * 统一的渠道预下单结果。
 * 用来屏蔽 mock、微信、支付宝之间的字段差异。
 */
@Data
public class ChannelPayResult {
    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 支付链接
     */
    private String payUrl;

    /**
     * 支付渠道第三方单号
     */
    private String channelTradeNo;

    /**
     * 错误信息
     */
    private String errorMsg;
}
