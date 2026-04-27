package org.lml.thirdService.mq.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseMessage<T> implements Serializable {
    /**
     * 唯一 ID，用于幂等和追踪
     */
    private String messageId;
    /**
     * 业务 Key（对应 Kafka Partition Key）
     */
    private String businessKey;
    /**
     * 来源系统
     */
    private String source;
    /**
     * 发送时间
     */
    private Long timestamp;
    /**
     * 业务具体的 DTO 数据
     */
    private T data;
}
