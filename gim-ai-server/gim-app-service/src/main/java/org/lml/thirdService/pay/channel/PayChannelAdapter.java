package org.lml.thirdService.pay.channel;

import org.lml.thirdService.pay.constant.PayChannel;
import org.lml.thirdService.pay.entity.req.DownloadBillChannelRequest;
import org.lml.thirdService.pay.entity.req.FundBillChannelRequest;
import org.lml.thirdService.pay.entity.req.TradeBillChannelRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 支付渠道 SPI。
 * 不同渠道只需要实现这层接口，上层编排不直接依赖具体 SDK。
 */
public interface PayChannelAdapter {
    /**
     * 返回当前适配器支持的支付渠道。
     *
     * @return 支付渠道
     */
    PayChannel getChannel();

    /**
     * 支付
     */
    ChannelPayResult pay(ChannelPrePayRequest request);

    /**
     * todo: 查询支付结果
     */
    ChannelPayResult payResultQuery(String channelStreamId);

    /**
     * todo: 支付结果回调
     */
    boolean notify(HttpServletRequest request, HttpServletResponse response);

    /**
     * todo: 退款
     */
    ChannelPayResult refund();

    /**
     * todo: 退款结果回调
     *
     * @param request
     * @param response
     * @return
     */
    boolean refundNotify(HttpServletRequest request, HttpServletResponse response);
    /**
     * todo: 交易账单
     *
     * @param billChannelRequest
     * @return
     */
    ChannelPayResult tradeBill(TradeBillChannelRequest billChannelRequest);

    /**
     * todo: 资金账单
     *
     * @param billChannelRequest
     * @return
     */
    ChannelPayResult fundBill(FundBillChannelRequest billChannelRequest);

    /**
     * todo: 下载账单
     *
     * @param request
     * @return
     */
    ChannelPayResult downloadBill(DownloadBillChannelRequest request);
}
