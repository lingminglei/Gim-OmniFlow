package org.lml.thirdService.pay.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.lml.thirdService.mq.entity.BaseMessage;
import org.lml.thirdService.mq.enums.MqBusinessEnum;
import org.lml.thirdService.mq.producer.StreamProducer;
import org.lml.thirdService.pay.entity.event.PayCancelRequestedEvent;
import org.lml.thirdService.pay.entity.event.RechargeCreditEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
/**
 * 支付相关MQ生产者。
 * 统一封装取消回滚和充值到账两类事件的发送逻辑。
 */
public class PayMqProducer {
    /**
     * 支付取消消息输出绑定。
     */
    private static final String PAY_CANCEL_BINDING = "payCancelProducer-out-0";

    /**
     * 充值到账消息输出绑定。
     */
    private static final String RECHARGE_CREDIT_BINDING = "rechargeCreditProducer-out-0";

    @Resource
    private StreamProducer streamProducer;

    /**
     * 发送支付取消事件到MQ，供异步回滚链路消费。
     *
     * @param event 支付取消事件
     */
    public void sendCancelEvent(PayCancelRequestedEvent event) {
        log.info("开始发送支付取消消息，事务号={}, 支付单号={}, 业务单号={}, 重试次数={}",
                event.getTransactionId(), event.getPayOrderId(), event.getBizNo(), event.getRetryCount());

        // 用支付单号作为业务键，便于消费端按订单维度做幂等控制。
        BaseMessage<PayCancelRequestedEvent> message = BaseMessage.<PayCancelRequestedEvent>builder()
                .messageId(IdUtil.fastSimpleUUID())
                .businessKey(event.getPayOrderId())
                .source(MqBusinessEnum.PAY_CANCEL_REQUESTED.getCode())
                .timestamp(System.currentTimeMillis())
                .data(event)
                .build();

        boolean sent = streamProducer.send(PAY_CANCEL_BINDING, null, JSON.toJSONString(message));
        log.info("支付取消消息发送完成，消息ID={}, 绑定名称={}, 支付单号={}, 发送结果={}",
                message.getMessageId(), PAY_CANCEL_BINDING, event.getPayOrderId(), sent);
        if (!sent) {
            throw new IllegalStateException("发送支付取消消息失败");
        }
    }
}
