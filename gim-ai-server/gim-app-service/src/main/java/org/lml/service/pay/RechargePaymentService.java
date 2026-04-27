package org.lml.service.pay;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.enums.pay.BizType;
import org.lml.common.enums.pay.RechargePackage;
import org.lml.common.exection.BizException;
import org.lml.thirdService.order.constant.TradeOrderState;
import org.lml.thirdService.order.entity.dto.TradeOrder;
import org.lml.thirdService.order.entity.dto.TradeOrderStream;
import org.lml.thirdService.order.entity.event.TradeOrderEvent;
import org.lml.thirdService.order.entity.req.OrderConfirmRequest;
import org.lml.thirdService.order.entity.req.OrderPayRequest;
import org.lml.thirdService.pay.channel.ChannelPayResult;
import org.lml.thirdService.pay.channel.ChannelPrePayRequest;
import org.lml.thirdService.pay.channel.PayChannelAdapter;
import org.lml.thirdService.pay.channel.PayChannelAdapterFactory;
import org.lml.thirdService.pay.constant.BizOrderType;
import org.lml.thirdService.pay.constant.PayOrderState;
import org.lml.thirdService.pay.constant.UserType;
import org.lml.thirdService.pay.entity.dto.PayOrder;
import org.lml.thirdService.pay.entity.event.PayCancelRequestedEvent;
import org.lml.thirdService.pay.entity.event.PaySuccessEvent;
import org.lml.thirdService.pay.entity.event.RechargeCreditEvent;
import org.lml.thirdService.pay.entity.req.MockPayNotifyRequest;
import org.lml.thirdService.pay.entity.req.PayCreateRequest;
import org.lml.thirdService.pay.entity.req.RechargeCancelRequest;
import org.lml.thirdService.pay.entity.req.RechargeConfirmRequest;
import org.lml.thirdService.pay.entity.req.RechargeTryPayRequest;
import org.lml.thirdService.pay.entity.resp.RechargeTryPayResponse;
import org.lml.thirdService.pay.service.PayMqProducer;
import org.lml.thirdService.tcc.entity.TransTrySuccessType;
import org.lml.thirdService.tcc.request.TccRequest;
import org.lml.thirdService.tcc.response.TransactionCancelResponse;
import org.lml.thirdService.tcc.response.TransactionConfirmResponse;
import org.lml.thirdService.tcc.response.TransactionTryResponse;
import org.lml.thirdService.tcc.service.TransactionLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

import static org.lml.common.constant.CommonConstant.BASE_USER_ID;
import static org.lml.common.enums.pay.RechargePackage.fromCode;

@Service
@Slf4j
/**
 * 充值支付编排服务。
 * 按照“建单预占 -> 发起支付 -> 回调终态”三个阶段组织充值支付链路。
 */
public class RechargePaymentService {

    /**
     * 取消回调标记。
     */
    private static final String PAY_CANCELLED = "CANCELLED";

    private static final int MAX_RETRY_TIMES = 2;

    @Resource
    private IPayOrderService payOrderService;
    @Resource
    private ITradeOrderService tradeOrderService;
    @Resource
    private ITradeOrderStreamService tradeOrderStreamService;

    @Resource
    private PayChannelAdapterFactory adapterFactory;
    @Resource
    private TransactionLogService transactionLogService;
    @Resource
    private PayMqProducer payMqProducer;
    @Resource
    private RechargeCreditService rechargeCreditService;

    /**
     * 执行支付尝试阶段。
     * 只负责创建交易单、创建支付单、生成事务号，并把本次充值积分预占到冻结余额，
     * 不在这里发起支付，也不推进订单到终态。
     *
     * @param request 尝试支付请求
     * @return 建单结果
     */
    @Transactional(rollbackFor = Exception.class)
    public RechargeTryPayResponse tryRecharge(RechargeTryPayRequest request) {
        log.info("开始执行充值支付尝试阶段，套餐编码={}, 幂等号={}, 支付渠道={}",
                request.getPackageCode(), request.getIdentifier(), request.getPayChannel());

        // 通过 identifier 做幂等号查询，避免前端重复点击导致重复建单。
        QueryWrapper<TradeOrder> idempotentQuery = new QueryWrapper<>();
        idempotentQuery.eq("identifier", request.getIdentifier())
                .eq("biz_scene", BizType.TRY_PAY.getBizType());
        TradeOrder existingTradeOrder = tradeOrderService.getOne(idempotentQuery);
        if (existingTradeOrder != null) {
            PayOrder existedPay = getPayOrderByBizNo(existingTradeOrder.getOrderId());
            log.info("充值支付尝试阶段命中幂等，直接复用已有订单，幂等号={}, 支付单号={}, 交易单号={}",
                    request.getIdentifier(), existedPay.getPayOrderId(), existingTradeOrder.getOrderId());
            return buildResponse(existingTradeOrder.getOrderId(), existedPay.getPayUrl());
        }

        //预生成订单号
        String orderId = String.valueOf(IdUtil.getSnowflake().nextId());

        //业务套餐：
        RechargePackage rechargePackage = fromCode(request.getPackageCode());

        TradeOrder tradeOrder = new TradeOrder();
        /**
         * 做订单的创建，订单创建失败，则取消订单
         */
        boolean isTrySuccess = true;
        try{
            //订单的创建
            tradeOrder = buildTradeOrder(orderId, request, rechargePackage);
            tradeOrderService.save(tradeOrder);

            //订单流水的创建
            TradeOrderStream orderStream = new TradeOrderStream(tradeOrder, TradeOrderEvent.CREATE, request.getIdentifier());
            boolean result = tradeOrderStreamService.save(orderStream);
            Assert.isTrue(result, "订单流水创建失败");

        } catch (Exception e) {
            isTrySuccess = false;
            log.error("normalBuy try failed, ", e);
        }

        //todo: Try失败，发【废单消息】，异步进行逆向补偿，推进订单状态-
        if (!isTrySuccess) {
            //消息监听： NormalBuyMsgListener
//            streamProducer.send("normalBuyCancel-out-0", orderCreateRequest.getGoodsType().name(), JSON.toJSONString(orderCreateRequest));
//            return new OrderResponse.OrderResponseBuilder().buildFail(NORMAL_BUY_TCC_CANCEL_FAILED.getCode(), NORMAL_BUY_TCC_CANCEL_FAILED.getMessage());
        }

        RechargeTryPayResponse rechargeTryPayResponse = new RechargeTryPayResponse();
        rechargeTryPayResponse.setPayOrderId(tradeOrder.getOrderId());
        return rechargeTryPayResponse;
    }

    /**
     * 执行支付确认阶段。
     * 前端拿着尝试阶段生成的订单信息来发起实际支付，这里只负责推进到待回调状态，
     * 不直接把订单改成已支付，也不发送到账消息。
     *
     *
     * 1、查询订单信息
     *
     * 2、创建支付单信息
     *
     * 3、调用第三方发起支付，获取支付链接
     *
     * 4、推进订单、支付单 状态
     *
     * @param request 确认支付请求
     * @return 发起支付后的结果
     */
    @Transactional(rollbackFor = Exception.class)
    public RechargeTryPayResponse confirm(RechargeConfirmRequest request) {
        log.info("开始执行充值支付确认阶段，事务号={}, 支付单号={}, 支付渠道={}", request.getPayOrderId(), request.getPayChannel());
        String userId = (String) StpUtil.getLoginId();

        //查询订单信息
        TradeOrder tradeOrder = getTradeOrderByOrderId(request.getPayOrderId());
        if (tradeOrder.getOrderState() != TradeOrderState.CREATE) {
            throw new IllegalStateException("当前交易单状态不允许执行确认支付: " + tradeOrder.getOrderState());
        }

        if (!tradeOrder.getBuyerId().equals(userId)) {
            throw new BizException("用户无支付权限");
        }

        PayOrder payOrder = buildPayOrder(request,tradeOrder);

        // 统一通过渠道适配器发起下单
        PayChannelAdapter adapter = adapterFactory.getAdapter(request.getPayChannel());
        ChannelPayResult channelPayResult = adapter.pay(ChannelPrePayRequest.builder()
                .payOrderId(payOrder.getPayOrderId())
                .amount(payOrder.getOrderAmount())
                .build());
        if (!channelPayResult.isSuccess()) {
            throw new IllegalStateException("渠道预下单失败: " + channelPayResult.getErrorMsg());
        }

//        ensureTradeOrderConfirmed
        //订单状态推进
        tradeOrder.confirm();
        tradeOrderService.updateById(tradeOrder);
        //订单流水插入
        tradeOrderStreamService.save(new TradeOrderStream(tradeOrder, TradeOrderEvent.CONFIRM, tradeOrder.getIdentifier()));

        //支付订单-推进状态-支付单据创建
        payOrder.setPayChannel(request.getPayChannel());
        payOrder.paying(channelPayResult.getPayUrl());
        payOrder.setChannelTradeNo(channelPayResult.getChannelTradeNo());
        payOrderService.save(payOrder);

        log.info("充值支付确认阶段完成，事务号={}, 支付单号={}, 支付链接={}, 支付单状态={}",
                payOrder.getPayOrderId(), payOrder.getPayUrl(), payOrder.getOrderState());
        return buildResponse(request.getPayOrderId(), tradeOrder.getOrderId());
    }

    /**
     * 发起充值支付取消。
     * 这里只负责发送取消事件，由MQ消费端异步推进终态和释放预占积分。
     *
     * @param request 取消请求
     */
    public void cancelRecharge(RechargeCancelRequest request) {
        log.info("开始发起充值支付取消，事务号={}, 支付单号={}, 原因={}",
                request.getTransactionId(), request.getPayOrderId(), request.getReason());

        PayOrder payOrder = getPayOrderByPayOrderId(request.getPayOrderId());
//        assertTransactionMatch(request.getTransactionId(), payOrder);
        if (payOrder.getOrderState() == PayOrderState.PAID || payOrder.getOrderState() == PayOrderState.REFUNDED) {
            log.info("充值支付取消跳过，支付单已处于成功终态，支付单号={}", payOrder.getPayOrderId());
            return;
        }

        PayCancelRequestedEvent event = new PayCancelRequestedEvent();
        event.setTransactionId(request.getTransactionId());
        event.setPayOrderId(request.getPayOrderId());
        event.setBizNo(payOrder.getBizNo());
        event.setRetryCount(0);
        event.setReason(request.getReason());
        payMqProducer.sendCancelEvent(event);

        log.info("充值支付取消事件已发送，事务号={}, 支付单号={}, 业务单号={}",
                request.getTransactionId(), request.getPayOrderId(), payOrder.getBizNo());
    }

    /**
     * 处理模拟支付回调。
     * 成功回调只发送支付成功终态MQ，失败回调只发送取消MQ，
     * 由消费端统一落库，避免接口层直接推进最终状态。
     *
     * @param request 模拟回调参数
     */
    public void onMockNotify(MockPayNotifyRequest request) {

    }

    /**
     * 在MQ消费端完成支付失败或取消终态处理。
     * 这里统一推进TCC cancel、关闭订单，并释放预占积分。
     *
     * @param event 支付取消事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void finalizeRechargeCancel(PayCancelRequestedEvent event) {
        log.info("开始处理充值支付取消终态，事务号={}, 支付单号={}, 重试次数={}",
                event.getTransactionId(), event.getPayOrderId(), event.getRetryCount());

        PayOrder payOrder = getPayOrderByPayOrderId(event.getPayOrderId());
        if (payOrder.getOrderState() == PayOrderState.PAID || payOrder.getOrderState() == PayOrderState.REFUNDED) {
            log.info("充值支付取消终态跳过，支付单已处于成功终态，支付单号={}", payOrder.getPayOrderId());
            return;
        }
        if (payOrder.getOrderState() == PayOrderState.FAILED && PAY_CANCELLED.equals(payOrder.getNotifyStatus())) {
            log.info("充值支付取消终态跳过，支付单已完成取消处理，支付单号={}", payOrder.getPayOrderId());
            return;
        }

        TransactionCancelResponse cancelResponse = transactionLogService.cancelTransaction(
                new TccRequest(event.getTransactionId(), BizType.TRY_PAY.getBizType(), BizType.CONFIRM_PAY.getBizType()));
        if (!Boolean.TRUE.equals(cancelResponse.getSuccess())) {
            throw new IllegalStateException("分布式事务取消阶段执行失败");
        }

        rechargeCreditService.releaseRechargeCredits(payOrder.getPayerId(), payOrder.getPayOrderId(), payOrder.getCreditAmountSnapshot());

        payOrder.setOrderState(PayOrderState.FAILED);
        payOrder.setPayFailedTime(new Date());
        payOrder.setNotifyStatus(PAY_CANCELLED);
        payOrder.setNotifyTime(new Date());
        payOrderService.updateById(payOrder);

        TradeOrder tradeOrder = getTradeOrderByOrderId(payOrder.getBizNo());
        if (tradeOrder.getOrderState() != TradeOrderState.CLOSED
                && tradeOrder.getOrderState() != TradeOrderState.DISCARD
                && tradeOrder.getOrderState() != TradeOrderState.FINISH) {
            tradeOrder.setOrderState(TradeOrderState.CLOSED);
            tradeOrder.setOrderClosedTime(new Date());
            tradeOrder.setCloseType(TradeOrderEvent.CANCEL.name());
            tradeOrderService.updateById(tradeOrder);
            tradeOrderStreamService.save(new TradeOrderStream(tradeOrder, TradeOrderEvent.CANCEL, event.getTransactionId()));
        }

        log.info("充值支付取消终态处理完成，事务号={}, 支付单号={}, 交易单号={}",
                event.getTransactionId(), event.getPayOrderId(), payOrder.getBizNo());
    }

    /**
     * 查询支付单详情。
     *
     * @param payOrderId 支付单号
     * @return 支付单
     */
    public PayOrder queryPayOrder(String payOrderId) {
        log.info("开始查询充值支付单，支付单号={}", payOrderId);
        PayOrder payOrder = getPayOrderByPayOrderId(payOrderId);
        log.info("充值支付单查询完成，支付单号={}, 当前状态={}", payOrderId, payOrder.getOrderState());
        return payOrder;
    }

    /**
     * 组装交易订单。
     */
    private TradeOrder buildTradeOrder(String orderId, RechargeTryPayRequest request, RechargePackage rechargePackage) {
        String userId = (String) StpUtil.getLoginId();

        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(orderId);
        tradeOrder.setBuyerId(userId);
        tradeOrder.setBuyerType(UserType.CUSTOMER);
        tradeOrder.setSellerId(BASE_USER_ID);
        tradeOrder.setSellerType(UserType.PLATFORM);
        tradeOrder.setIdentifier(request.getIdentifier());//幂等号
        tradeOrder.setItemCount(1);//商品数量
        tradeOrder.setItemPrice(rechargePackage.getPrice());//商品单价
        tradeOrder.setPaidAmount(BigDecimal.ZERO);//已支付金额
        tradeOrder.setOrderAmount(new BigDecimal(tradeOrder.getItemCount()).multiply(tradeOrder.getItemPrice()));//订单金额
        tradeOrder.setGoodsId(rechargePackage.getCode());//商品类型编码
        tradeOrder.setGoodsName(rechargePackage.getDisplayName());//商品名称
        tradeOrder.setCreditAmount(rechargePackage.getCreditAmount());//商品对应积分数量
        tradeOrder.setBizScene(BizType.TRY_PAY.getBizType());
        tradeOrder.setOrderState(TradeOrderState.CREATE);//订单状态初始化为 - 创建
        tradeOrder.setRechargePackage(rechargePackage.getCode());//业务套餐
        return tradeOrder;
    }

    /**
     * 组装支付订单。
     *
     * @return 支付订单
     */
    private PayOrder buildPayOrder(RechargeConfirmRequest request,TradeOrder tradeOrder) {

        RechargePackage rechargePackage = fromCode(tradeOrder.getRechargePackage());
        String userId = (String) StpUtil.getLoginId();
        String orderId = tradeOrder.getOrderId();

        //支付订单的创建
        PayOrder payOrder = new PayOrder();
        payOrder.setPayerId(userId);//付款方ID
        payOrder.setPayerType(UserType.CUSTOMER);
        payOrder.setPayeeId(BASE_USER_ID);//收款方ID
        payOrder.setPayeeType(UserType.PLATFORM);

        payOrder.setBizNo(orderId);
        payOrder.setBizType(BizOrderType.RECHARGE);
        payOrder.setOrderAmount(rechargePackage.getPrice());//订单金额
        payOrder.setPayChannel(request.getPayChannel());//支付渠道
        payOrder.setMemo(request.getRemark() == null ? "积分充值" : request.getRemark());//支付备注信息

        payOrder.setOrderState(PayOrderState.TO_PAY);//订单初始化-待支付
        payOrder.setPayOrderId(tradeOrder.getOrderId());//支付单号
        payOrder.setPaidAmount(BigDecimal.ZERO);//已支付金额
        payOrder.setRefundedAmount(BigDecimal.ZERO);//已退款金额
        payOrder.setCurrency("CNY");

        payOrder.setPackageCode(rechargePackage.getCode());
        payOrder.setPackageNameSnapshot(rechargePackage.getDisplayName());
        payOrder.setCreditAmountSnapshot(rechargePackage.getCreditAmount());
        payOrder.setPriceSnapshot(rechargePackage.getPrice());
        return payOrder;
    }

    /**
     * 确保交易订单进入已确认待支付状态。
     * 如果订单还处于CREATE，则在这里推进到CONFIRM，并补一条订单流水。
     *
     * @param tradeOrder 交易订单
     */
    private void ensureTradeOrderConfirmed(TradeOrder tradeOrder) {

        if (tradeOrder.getOrderState() == TradeOrderState.CREATE) {
            throw new IllegalStateException("当前交易单状态不允许执行确认支付: " + tradeOrder.getOrderState());
        }

        //推进订单状态为确认操作
        tradeOrder.confirm();
        tradeOrderService.updateById(tradeOrder);

        //支付订单流水
        tradeOrderStreamService.save(new TradeOrderStream(tradeOrder, TradeOrderEvent.CONFIRM, tradeOrder.getIdentifier()));
    }

    /**
     * 组装支付成功终态事件。
     *
     * @param payOrder 支付单
     * @param channelTradeNo 渠道交易流水号
     * @return 充值到账事件
     */
    private RechargeCreditEvent buildRechargeCreditEvent(PayOrder payOrder, String channelTradeNo) {
        RechargeCreditEvent event = new RechargeCreditEvent();
        event.setPayOrderId(payOrder.getPayOrderId());
        event.setPackageCode(payOrder.getPackageCode());
        event.setCreditAmount(payOrder.getCreditAmountSnapshot());
        event.setOrderAmount(payOrder.getOrderAmount());
        event.setChannel(payOrder.getPayChannel());
        event.setChannelTradeNo(channelTradeNo);
        return event;
    }

    /**
     * 组装统一返回结果。
     *
     * @return 返回结果
     */
    private RechargeTryPayResponse buildResponse(String orderId, String payUrl) {
        return new RechargeTryPayResponse(
                orderId,
                payUrl);
    }

    /**
     * 按支付单号查询支付单。
     *
     * @param payOrderId 支付单号
     * @return 支付单
     */
    private PayOrder getPayOrderByPayOrderId(String payOrderId) {
        QueryWrapper<PayOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pay_order_id", payOrderId);
        PayOrder payOrder = payOrderService.getOne(queryWrapper);
        if (payOrder == null) {
            throw new IllegalArgumentException("未找到对应的支付单: " + payOrderId);
        }
        return payOrder;
    }

    /**
     * 按业务单号查询充值支付单。
     *
     * @param bizNo 业务单号
     * @return 支付单
     */
    private PayOrder getPayOrderByBizNo(String bizNo) {
        QueryWrapper<PayOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("biz_no", bizNo)
                .eq("biz_type", BizOrderType.RECHARGE.name());
        PayOrder payOrder = payOrderService.getOne(queryWrapper);
        if (payOrder == null) {
            throw new IllegalArgumentException("未找到对应的支付单，业务单号: " + bizNo);
        }
        return payOrder;
    }

    /**
     * 按订单号查询交易单。
     *
     * @param orderId 订单号
     * @return 交易单
     */
    private TradeOrder getTradeOrderByOrderId(String orderId) {
        QueryWrapper<TradeOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        TradeOrder tradeOrder = tradeOrderService.getOne(queryWrapper);
        if (tradeOrder == null) {
            throw new IllegalArgumentException("未找到对应的订单: " + orderId);
        }
        return tradeOrder;
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 当前登录用户ID
     */
    private String currentUserId() {
        return String.valueOf(StpUtil.getLoginId());
    }
}
