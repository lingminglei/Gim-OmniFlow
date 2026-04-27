package org.lml.thirdService.mq.streamRocketMq;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.lml.service.pay.RechargePaymentService;
import org.lml.thirdService.mq.entity.BaseMessage;
import org.lml.thirdService.mq.entity.MessageBody;
import org.lml.thirdService.pay.entity.event.PayCancelRequestedEvent;
import org.lml.thirdService.pay.entity.event.RechargeCreditEvent;
import org.lml.thirdService.pay.service.PayMqProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import javax.annotation.Resource;
import java.util.function.Consumer;

import static org.lml.thirdService.mq.consumer.AbstractStreamConsumer.getMessage;

@Configuration
@Slf4j
/**
 * 支付相关MQ消费监听器。
 * 统一消费支付成功和支付取消两类终态消息，再委派给支付编排服务处理业务状态。
 */
public class PayStreamTaskListener {
    /**
     * 取消消息最大重试次数。
     */
    private static final int MAX_RETRY = 5;

    @Resource
    private RechargePaymentService rechargePaymentService;
    @Resource
    private PayMqProducer payMqProducer;

    /**
     * 消费支付成功终态消息。
     * MQ层只负责解析消息和转发调用，真正的订单终态落库在服务层完成。
     *
     * @return 消费函数
     */
    @Bean
    public Consumer<Message<MessageBody>> rechargeCreditConsumer() {
        return message -> {
            BaseMessage<?> baseMessage = getMessage(message, BaseMessage.class);
            RechargeCreditEvent event = JSONUtil.toBean(JSONUtil.parseObj(baseMessage.getData()), RechargeCreditEvent.class);
//            log.info("开始消费充值到账消息，消息ID={}, 支付单号={}, 用户ID={}",
//                    baseMessage.getMessageId(), event.getPayOrderId(), event.getUserId());
//            rechargePaymentService.finalizeRechargeSuccess(event);
//            log.info("充值到账消息消费完成，支付单号={}, 用户ID={}",
//                    event.getPayOrderId(), event.getUserId());
        };
    }

    /**
     * 消费支付取消终态消息。
     *
     * @return 消费函数
     */
    @Bean
    public Consumer<Message<MessageBody>> payCancelConsumer() {
        return message -> {
            BaseMessage<?> baseMessage = getMessage(message, BaseMessage.class);
            PayCancelRequestedEvent event = JSONUtil.toBean(JSONUtil.parseObj(baseMessage.getData()), PayCancelRequestedEvent.class);
            handlePayCancelEvent(baseMessage.getMessageId(), event);
        };
    }

    /**
     * 处理支付取消消息。
     * 如果本次处理失败，则重新投递取消消息进行重试。
     *
     * @param messageId 消息ID
     * @param event 取消事件
     */
    public void handlePayCancelEvent(String messageId, PayCancelRequestedEvent event) {
        log.info("开始消费支付取消消息，消息ID={}, 事务号={}, 支付单号={}, 当前重试次数={}",
                messageId, event.getTransactionId(), event.getPayOrderId(), event.getRetryCount());
        try {
            rechargePaymentService.finalizeRechargeCancel(event);
            log.info("支付取消消息消费完成，事务号={}, 支付单号={}",
                    event.getTransactionId(), event.getPayOrderId());
        } catch (Exception ex) {
            int nextRetry = event.getRetryCount() + 1;
            if (nextRetry <= MAX_RETRY) {
                event.setRetryCount(nextRetry);
                payMqProducer.sendCancelEvent(event);
                log.warn("支付取消消息处理失败，已重新投递重试消息，支付单号={}, 重试次数={}",
                        event.getPayOrderId(), nextRetry);
            } else {
                log.error("支付取消消息重试次数已耗尽，事件内容={}", JSONUtil.toJsonStr(event), ex);
            }
            throw ex;
        }
    }
}
