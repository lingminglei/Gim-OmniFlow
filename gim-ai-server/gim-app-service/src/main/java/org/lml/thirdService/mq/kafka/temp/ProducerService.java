//package org.lml.thirdService.mq.kafka.temp;
//
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//public class ProducerService {
//
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    // 构造器注入 KafkaTemplate
//    public ProducerService(KafkaTemplate<String, String> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    // 发送消息到 Kafka
//    public void sendMessage(String topic, String message) {
//        kafkaTemplate.send(topic, message);
//        System.out.println("Sent message: " + message);
//    }
//}
