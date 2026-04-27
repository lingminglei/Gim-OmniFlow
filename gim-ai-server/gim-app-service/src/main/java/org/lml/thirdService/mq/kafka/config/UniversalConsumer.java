//package org.lml.thirdService.mq.kafka.config;
//
//import cn.hutool.json.JSONUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.lml.thirdService.mq.kafka.Handler.MessageHandler;
//import org.lml.thirdService.mq.entity.BaseMessage;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * TODO: 这里先用一个 topic 作为统一入口，后面在扩展处理为多topic情况
// */
//@Component
//@Slf4j
//public class UniversalConsumer {
//
//    // 自动注入所有实现类，按业务类型映射
//    private final Map<String, MessageHandler> handlerMap = new ConcurrentHashMap<>();
//
//    @Autowired
//    public UniversalConsumer(List<MessageHandler> handlers) {
//        handlers.forEach(h -> handlerMap.put(h.getSupportedType(), h));
//    }
//
//    @KafkaListener(
//            topics = "${app.kafka.topics.external-api}",
//            groupId = "${spring.kafka.consumer.group-id}"
//    )
//    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
//        try {
//            // 1. 解析基础消息结构
//            // 从 Kafka 拿到的是字符串 record.value()
//            String jsonStr = record.value();
//
//            // 使用 Hutool 将字符串解析回 BaseMessage 对象
//            // 注意：如果 data 是泛型，此时 data 字段会被解析为 JSONObject
//            BaseMessage<?> rawMessage = JSONUtil.toBean(jsonStr, BaseMessage.class);
//
//            // 2. 路由到具体的处理器
//            MessageHandler handler = handlerMap.get(rawMessage.getSource()); // 假设 source 是业务类型
//            if (handler != null) {
//                handler.handle(rawMessage);
//            } else {
//                log.warn("未找到对应的处理器: {}", rawMessage.getSource());
//            }
//
//            ack.acknowledge(); // 只有处理成功才提交 offset
//        } catch (Exception e) {
//            log.error("消费失败，进入重试或死信逻辑: {}", record.value(), e);
//            // 此处可抛出异常触发 Kafka 重试机制
//        }
//    }
//}
