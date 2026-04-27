package org.lml.thirdService.pay.channel;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcException;
import org.lml.billing.api.BillingFacadeService;
import org.lml.billing.enums.BillingOperationStatus;
import org.lml.billing.req.BillingRechargeReq;
import org.lml.billing.resp.BillingRechargeResp;
import org.lml.common.result.CommonResult;
import org.lml.mapper.pay.PayOrderMapper;
import org.lml.service.pay.IPayOrderService;
import org.lml.service.pay.ITradeOrderService;
import org.lml.service.pay.RechargeCreditService;
import org.lml.thirdService.order.constant.TradeOrderState;
import org.lml.thirdService.order.entity.dto.TradeOrder;
import org.lml.thirdService.order.entity.req.OrderPayRequest;
import org.lml.thirdService.order.statemachine.OrderStateMachine;
import org.lml.thirdService.pay.constant.PayChannel;
import org.lml.thirdService.pay.constant.PayOrderState;
import org.lml.thirdService.pay.entity.dto.PayOrder;
import org.lml.thirdService.pay.entity.event.PaySuccessEvent;
import org.lml.thirdService.pay.entity.event.RechargeCreditEvent;
import org.lml.thirdService.pay.entity.req.DownloadBillChannelRequest;
import org.lml.thirdService.pay.entity.req.FundBillChannelRequest;
import org.lml.thirdService.pay.entity.req.TradeBillChannelRequest;
import org.lml.thirdService.pay.service.PayMqProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@Slf4j
/**
 * 模拟支付渠道适配器。
 * 返回本地模拟支付链接和模拟渠道流水号，方便前后端联调。
 */
public class MockPayChannelAdapter implements PayChannelAdapter {


    @DubboReference(check = false, timeout = 5000, retries = 0, version = "1.0.0")
    private BillingFacadeService billingFacadeService;

    public static TransmittableThreadLocal<Map> context = new TransmittableThreadLocal<>();

    private static ThreadFactory chainResultProcessFactory = new ThreadFactoryBuilder()
            .setNameFormat("pay-process-pool-%d").build();

    ScheduledExecutorService scheduler = TtlExecutors.getTtlScheduledExecutorService(new ScheduledThreadPoolExecutor(10, chainResultProcessFactory));

    @Resource
    private IPayOrderService ipayOrderService;

    @Resource
    private ITradeOrderService tradeOrderService;

    @Resource
    private PayMqProducer payMqProducer;

    @Resource
    private RechargeCreditService rechargeCreditService;
    @Autowired
    private PayOrderMapper payOrderMapper;

    /**
     * 返回当前适配器支持的支付渠道。
     *
     * @return 模拟支付渠道
     */
    @Override
    public PayChannel getChannel() {
        log.debug("获取模拟支付渠道适配器标识");
        return PayChannel.MOCK;
    }

    @Override
    public ChannelPayResult pay(ChannelPrePayRequest request) {

        ChannelPayResult payChannelResponse = new ChannelPayResult();
        payChannelResponse.setSuccess(true);
        payChannelResponse.setPayUrl("http://www.nfturbo.com");
        Map<String, Serializable> params = new HashMap<>(12);
        params.put("payOrderId", request.getPayOrderId());
        params.put("paidAmount", request.getAmount());
        String userId = (String) StpUtil.getLoginId();
        params.put("userId", userId);
        context.set(params);

        //异步线程延迟15秒钟之后调用 notify 方法
        scheduler.schedule(() -> {
            this.notify(null, null);
        }, 30, TimeUnit.SECONDS);

        return payChannelResponse;

    }

    @Override
    public ChannelPayResult payResultQuery(String channelStreamId) {
        return null;
    }

    @Override
    public boolean notify(HttpServletRequest request, HttpServletResponse response) {

        try {
            PaySuccessEvent paySuccessEvent = new PaySuccessEvent();
            paySuccessEvent.setChannelStreamId(UUID.randomUUID().toString());
            Map<String, Serializable> params = (Map<String, Serializable>) context.get();

            BigDecimal amount = (BigDecimal) params.get("paidAmount");
            paySuccessEvent.setPaidAmount(amount);
            paySuccessEvent.setPayOrderId((String) params.get("payOrderId"));//支付订单号

            String userId = (String) params.get("userId");
            paySuccessEvent.setPayer(userId);
            paySuccessEvent.setPaySucceedTime(new Date());
            paySuccessEvent.setPayChannel(PayChannel.MOCK);
            this.paySuccess(paySuccessEvent);
        } catch (Exception e) {
            log.error("nofity error", e);
            return false;
        }
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


    /**
     * 支付成功
     * <pre>
     *     正常支付成功：
     *     1、查询订单状态
     *     2、推进订单状态到支付成功
     *     3、积分新增
     *     4、推进支付状态到支付成功
     *
     *     支付幂等成功：
     *      1、查询订单状态
     *      2、推进支付状态到支付成功
     *
     *      重复支付：
     *      1、查询订单状态
     *      2、创建退款单
     *      3、重试退款直到成功
     * </pre>
     */
    @Transactional(rollbackOn =  Exception.class)
    public boolean paySuccess(PaySuccessEvent paySuccessEvent) {

        //查询支付订单信息
        QueryWrapper<PayOrder> qw1 = new QueryWrapper<>();
        qw1.eq("pay_order_id", paySuccessEvent.getPayOrderId());
        PayOrder payOrder = ipayOrderService.getOne(qw1);
        if (payOrder.isPaid()) {
            return true;
        }

        //查询订单信息
        QueryWrapper<TradeOrder> qw2 = new QueryWrapper<>();
        qw2.eq("order_id", payOrder.getBizNo());
        TradeOrder tradeOrder = tradeOrderService.getOne(qw2);

        //如果订单已经被其他支付推进到支付成功，或者已经关单，则启动退款流程
        if (tradeOrder.equals(TradeOrderState.PAID)
                || tradeOrder.equals(TradeOrderState.CLOSED)
                || tradeOrder.equals(TradeOrderState.DISCARD)) {
            log.info("order already paid ,do chargeback ," + payOrder.getBizNo());

            doChargeBack(paySuccessEvent, tradeOrder);
            return true;
        }

        //处理积分新增
        try{
            BillingRechargeReq billingRechargeReq = buildBillingRecharge(paySuccessEvent,
                    tradeOrder.getCreditAmount());
            log.info("通过Dubbo 请求积分服务-实现积分充值业务.构建请求参数：{}",billingRechargeReq);
            CommonResult<BillingRechargeResp> result = billingFacadeService.recharge(billingRechargeReq);
            log.info("通过Dubbo 请求积分服务-实现积分充值业务.响应结果：{}",result);

            if (!result.getCode().equals(Integer.valueOf("200")) ||
                    !result.getData().getStatus().equals(BillingOperationStatus.SUCCESS)) {
                throw new IllegalStateException("通过Dubbo 请求积分服务-实现积分充值业务,操作失败！");
            }
        } catch (RpcException e) {
            log.error("积分服务通信异常，进入不确定状态", e);
        }

        //推进支付订单状态
        payOrder.paySuccess(paySuccessEvent);
        payOrderMapper.updateById(payOrder);

        //推进业务订单状态
        OrderPayRequest orderPayRequest = new OrderPayRequest();
        orderPayRequest.setOrderId(paySuccessEvent.getPayOrderId());
        orderPayRequest.setOperateTime(new Date());
        orderPayRequest.setPayChannel(paySuccessEvent.getPayChannel());
        orderPayRequest.setAmount(payOrder.getOrderAmount());
        tradeOrder.paySuccess(orderPayRequest);
        tradeOrderService.updateById(tradeOrder);
        return true;
    }

    /**
     * 构建积分扣减请求参数Req
     */
    private BillingRechargeReq buildBillingRecharge(PaySuccessEvent paySuccessEvent,Integer creditAmount) {
        BillingRechargeReq billingRechargeReq = new BillingRechargeReq();
        //这里通过【支付订单号】做幂等控制，因为 dubbo 有重试机制；
        billingRechargeReq.setRequestNo(paySuccessEvent.getPayOrderId());//幂等请求号；
        billingRechargeReq.setUserId(paySuccessEvent.getPayer());
        billingRechargeReq.setBizNo(paySuccessEvent.getPayOrderId());//业务单号
        billingRechargeReq.setSourceSystem("Gim-App-Service");//来源系统
        billingRechargeReq.setAmount(creditAmount);
        billingRechargeReq.setRemark("积分套餐充值-积分新增");

        return  billingRechargeReq;
    }

    /**
     * 单据退款
     */
    private void doChargeBack(PaySuccessEvent paySuccessEvent, TradeOrder tradeOrderVO) {

        //异步进行退款执行，失败了交给定时任务重试
        CompletableFuture.runAsync(() -> {
            // 你的业务逻辑，比如插入支付流水
            System.out.println("异步任务执行中...执行退款操作.");
        });
    }


}
