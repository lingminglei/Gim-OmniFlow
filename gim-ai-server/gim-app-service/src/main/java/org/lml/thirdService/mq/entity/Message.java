package org.lml.thirdService.mq.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 娑堟伅
 *
 * @author hollis
 */
@Data
@Accessors(chain = true)
public class Message {
    /**
     * 娑堟伅id
     */
    private String msgId;
    /**
     * 娑堟伅浣?
     */
    private String body;
}