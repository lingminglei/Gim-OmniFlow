package org.lml.thirdService.mq.producer;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.common.message.MessageConst;
import org.lml.thirdService.mq.entity.MessageBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * RocketMQ 消息生产者封装工具类
 * 基于 Spring Cloud Stream 的 StreamBridge 实现，支持普通消息、延迟消息及带自定义 Header 的消息发送。
 * * @author hollis
 */
public class StreamProducer {

    private static Logger logger = LoggerFactory.getLogger(StreamProducer.class);

    public static final int DELAY_LEVEL_1_M = 5;

    public static final int DELAY_LEVEL_30_S = 4;

    public static final String ROCKET_MQ_MESSAGE_ID = "ROCKET_MQ_MESSAGE_ID";

    public static final String ROCKET_TAGS = "ROCKET_TAGS";

    public static final String ROCKET_MQ_TOPIC = "ROCKET_MQ_TOPIC";

    @Resource
    private StreamBridge streamBridge;

    /**
     * 发送普通消息
     *
     * @param bingingName 绑定名称（对应 yml 中的 spring.cloud.stream.bindings.xxx-out-0）
     * @param tag         消息标签，用于消费者端进行过滤
     * @param msg         消息载荷内容
     * @return 发送结果（true 表示成功，false 表示失败）
     */
    public boolean send(String bingingName, String tag, String msg) {
        MessageBody message = new MessageBody()
                .setIdentifier(UUID.randomUUID().toString())
                .setBody(msg);

        logger.info("send message : {} , {} , {}", bingingName, tag, JSON.toJSONString(message));

        // 构建消息并设置 TAGS 头，以便 RocketMQ 的订阅过滤
        boolean result = streamBridge.send(bingingName, MessageBuilder.withPayload(message)
                .setHeader("TAGS", tag)
                .build());

        logger.info("send result : {} , {} , {}", bingingName, tag, result);
        return result;
    }

    /**
     * 发送延迟消息
     *
     * @param bingingName 绑定名称
     * @param tag         消息标签
     * @param msg         消息载荷内容
     * @param delayLevel  延迟级别 (1-18)，对应 RocketMQ 的预设延迟时间
     * @return 发送结果
     */
    public boolean send(String bingingName, String tag, String msg, int delayLevel) {
        MessageBody message = new MessageBody()
                .setIdentifier(UUID.randomUUID().toString())
                .setBody(msg);

        logger.info("send message : {} , {} , {}", bingingName, tag, JSON.toJSONString(message));

        // 设置 MESSAGE_DELAY_LEVEL 属性，通知 RocketMQ Broker 进行延迟投递
        boolean result = streamBridge.send(bingingName, MessageBuilder.withPayload(message)
                .setHeader("TAGS", tag)
                .setHeader(MessageConst.PROPERTY_DELAY_TIME_LEVEL, delayLevel)
                .build());

        logger.info("send result : {} , {} , {}", bingingName, tag, result);
        return result;
    }

    /**
     * 发送带有自定义 Header 的消息（例如透传分布式追踪ID、业务特殊标识等）
     *
     * @param bingingName  绑定名称
     * @param tag          消息标签
     * @param msg          消息载荷内容
     * @param headerKey    自定义 Header 的 Key
     * @param headerValue  自定义 Header 的 Value
     * @return 发送结果
     */
    public boolean send(String bingingName, String tag, String msg, String headerKey, String headerValue) {
        MessageBody message = new MessageBody()
                .setIdentifier(UUID.randomUUID().toString())
                .setBody(msg);

        logger.info("send message : {} , {}", bingingName, JSON.toJSONString(message));

        // 将自定义 Header 放入消息构建器中
        boolean result = streamBridge.send(bingingName, MessageBuilder.withPayload(message)
                .setHeader("TAGS", tag)
                .setHeader(headerKey, headerValue)
                .build());

        logger.info("send result : {} , {}", bingingName, result);
        return result;
    }
}
