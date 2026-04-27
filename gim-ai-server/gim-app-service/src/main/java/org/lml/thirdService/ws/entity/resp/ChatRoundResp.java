package org.lml.thirdService.ws.entity.resp;

import lombok.Data;

/**
 * 单轮会话历史返回对象。
 */
@Data
public class ChatRoundResp {

    /**
     * 用户原始输入内容。
     */
    private String user;

    /**
     * AI 回复内容。
     */
    private String ai;

    /**
     * 第几轮对话。
     */
    private Integer rounds;
}
