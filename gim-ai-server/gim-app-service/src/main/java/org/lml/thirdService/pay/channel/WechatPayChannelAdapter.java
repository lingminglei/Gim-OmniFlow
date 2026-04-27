package org.lml.thirdService.pay.channel;

import lombok.extern.slf4j.Slf4j;
import org.lml.thirdService.pay.constant.PayChannel;
import org.lml.thirdService.pay.entity.req.DownloadBillChannelRequest;
import org.lml.thirdService.pay.entity.req.FundBillChannelRequest;
import org.lml.thirdService.pay.entity.req.TradeBillChannelRequest;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
/**
 * 微信支付适配器占位实现。
 * 当前只保留扩展入口，后续接入真实微信支付能力。
 */
public class WechatPayChannelAdapter implements PayChannelAdapter {
    /**
     * 返回当前适配器支持的支付渠道。
     *
     * @return 微信支付渠道
     */
    @Override
    public PayChannel getChannel() {
        log.debug("获取微信支付适配器标识");
        return PayChannel.WECHAT;
    }

    @Override
    public ChannelPayResult pay(ChannelPrePayRequest request) {
        return null;
    }

    @Override
    public ChannelPayResult payResultQuery(String channelStreamId) {
        return null;
    }

    @Override
    public boolean notify(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    @Override
    public ChannelPayResult refund() {
        return null;
    }

    @Override
    public boolean refundNotify(HttpServletRequest request, HttpServletResponse response) {
        return false;
    }

    @Override
    public ChannelPayResult tradeBill(TradeBillChannelRequest billChannelRequest) {
        return null;
    }

    @Override
    public ChannelPayResult fundBill(FundBillChannelRequest billChannelRequest) {
        return null;
    }

    @Override
    public ChannelPayResult downloadBill(DownloadBillChannelRequest request) {
        return null;
    }

}
