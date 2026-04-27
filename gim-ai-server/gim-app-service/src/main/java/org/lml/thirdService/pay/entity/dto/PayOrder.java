package org.lml.thirdService.pay.entity.dto;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.order.entity.dto.TradeOrder;
import org.lml.thirdService.pay.constant.BizOrderType;
import org.lml.thirdService.pay.constant.PayChannel;
import org.lml.thirdService.pay.constant.PayOrderState;
import org.lml.thirdService.pay.constant.UserType;
import org.lml.thirdService.pay.entity.event.PaySuccessEvent;
import org.lml.thirdService.pay.entity.event.RefundSuccessEvent;
import org.lml.thirdService.pay.entity.req.PayCreateRequest;
import org.lml.thirdService.pay.entity.req.RechargeConfirmRequest;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付单
 *
 * @author Hollis
 */
@Setter
@Getter
@TableName("pay_order")
public class PayOrder {

    private String id;

    /**
     * 默认超时时间
     */
    public static final int DEFAULT_TIME_OUT_MINUTES = 30;

    /**
     * 支付单号
     */
    private String payOrderId;

    /**
     * 付款方id
     */
    private String payerId;

    /**
     * 付款方id类型
     */
    private UserType payerType;

    /**
     * 收款方id
     */
    private String payeeId;

    /**
     * 收款方id类型
     */
    private UserType payeeType;

    /**
     * 业务单号 - 订单单号
     */
    private String bizNo;

    /**
     * 业务单号类型
     */
    private BizOrderType bizType;

    /**
     * 充值套餐编码
     */
    private String packageCode;

    /**
     * 套餐名称快照
     */
    private String packageNameSnapshot;

    /**
     * 套餐积分快照
     */
    private Integer creditAmountSnapshot;

    /**
     * 套餐价格快照
     */
    private BigDecimal priceSnapshot;

    /**
     * 币种
     */
    private String currency;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 已支付金额
     */
    private BigDecimal paidAmount;

    /**
     * 已退款金额
     */
    private BigDecimal refundedAmount;

    /**
     * 外部支付流水号
     */
    private String channelStreamId;

    /**
     * 渠道交易号
     */
    private String channelTradeNo;

    /**
     * 退款渠道流水号
     */
    private String refundChannelStreamId;

    /**
     * 支付链接
     */
    private String payUrl;

    /**
     * 支付渠道
     */
    private PayChannel payChannel;

    /**
     * 支付备注
     */
    private String memo;

    /**
     * TCC事务号
     */
//    private String tccTransactionId;

    /**
     * 回调状态
     */
    private String notifyStatus;

    /**
     * 回调时间
     */
    private Date notifyTime;

    /**
     * 订单状态
     */
    private PayOrderState orderState;

    /**
     * 支付成功时间
     */
    private Date paySucceedTime;

    /**
     * 支付失败时间
     */
    private Date payFailedTime;

    /**
     * 支付超时时间
     */
    private Date payExpireTime;


    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @JSONField(serialize = false)
    public boolean isPaid() {
        return paidAmount.compareTo(BigDecimal.ZERO) > 0
                && (orderState == PayOrderState.PAID  || orderState == PayOrderState.REFUNDED)
                && channelStreamId != null
                && paySucceedTime != null;
    }

    @JSONField(serialize = false)
    public boolean isPayFailed() {
        return orderState == PayOrderState.FAILED;
    }


    public PayOrder paying(String payUrl) {
        Assert.equals(this.getOrderState(), PayOrderState.TO_PAY);
        this.setOrderState(PayOrderState.PAYING);
        this.payUrl = payUrl;
        return this;
    }

    public PayOrder paySuccess(PaySuccessEvent paySuccessEvent) {
        Assert.equals(this.getOrderState(), PayOrderState.PAYING);
        this.setOrderState(PayOrderState.PAID);
        this.paySucceedTime = paySuccessEvent.getPaySucceedTime();
        this.channelStreamId = paySuccessEvent.getChannelStreamId();
        this.channelTradeNo = paySuccessEvent.getChannelStreamId();
        this.paidAmount = paySuccessEvent.getPaidAmount();
        this.notifyStatus = "SUCCESS";
        this.notifyTime = new Date();
        return this;
    }

    public PayOrder payExpired() {
        Assert.equals(this.getOrderState(), PayOrderState.PAYING);
        this.setOrderState(PayOrderState.EXPIRED);
        this.payExpireTime = new Date();
        return this;
    }

    public PayOrder payFailed() {
        Assert.equals(this.getOrderState(), PayOrderState.PAYING);
        this.setOrderState(PayOrderState.FAILED);
        this.payFailedTime = new Date();
        return this;
    }

    public PayOrder refundSuccess(RefundSuccessEvent refundSuccessEvent) {
        Assert.equals(this.getOrderState(), PayOrderState.PAID);
        this.setOrderState(PayOrderState.REFUNDED);
        this.refundChannelStreamId = refundSuccessEvent.getChannelStreamId();
        this.refundedAmount = refundSuccessEvent.getRefundedAmount();
        return this;
    }
}
