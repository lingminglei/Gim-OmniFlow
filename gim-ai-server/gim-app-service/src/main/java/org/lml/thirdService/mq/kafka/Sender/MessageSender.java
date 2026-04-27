//package org.lml.thirdService.mq.kafka.Sender;
//
//import org.lml.thirdService.mq.entity.BaseMessage;
//import org.springframework.util.concurrent.FailureCallback;
//import org.springframework.util.concurrent.SuccessCallback;
//
//public interface MessageSender {
//    /**
//     * 发送异步消息
//     * @param topic 目标主题
//     * @param message 消息模版
//     */
//    <T> void send(String topic, BaseMessage<T> message);
//
//    /**
//     * 发送带回调的消息（用于重构时监控成功率）
//     */
//    <T> void send(String topic, BaseMessage<T> message, SuccessCallback successCallback, FailureCallback failureCallback);
//}
