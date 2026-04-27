//package org.lml.thirdService.mq.kafka.Sender.impl;
//
//import cn.hutool.json.JSONUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.lml.thirdService.mq.kafka.Sender.MessageSender;
//import org.lml.thirdService.mq.entity.BaseMessage;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.util.concurrent.FailureCallback;
//import org.springframework.util.concurrent.SuccessCallback;
//
//@Service
//@Slf4j
//public class KafkaMessageSenderImpl implements MessageSender {
//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    @Override
//    public <T> void send(String topic, BaseMessage<T> message) {
//        String jsonPayload = JSONUtil.toJsonStr(message);
//        // businessKey 作为 Kafka Key 确保相同业务 ID 进入相同分区（保证顺序）
//        kafkaTemplate.send(topic, message.getBusinessKey(), jsonPayload)
//            .addCallback(
//                result -> log.info("Message sent successfully: {}", message.getMessageId()),
//                ex -> log.error("Failed to send message: {}", message.getMessageId(), ex)
//            );
//    }
//
//    @Override
//    public <T> void send(String topic, BaseMessage<T> message, SuccessCallback successCallback, FailureCallback failureCallback) {
//
//    }
//}
