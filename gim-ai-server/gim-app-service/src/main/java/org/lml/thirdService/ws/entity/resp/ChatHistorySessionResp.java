package org.lml.thirdService.ws.entity.resp;

import lombok.Data;

import java.util.List;

/**
 * 单个会话历史返回对象。
 */
@Data
public class ChatHistorySessionResp {

    /**
     * 前端传入的会话 ID。
     */
    private String conversationId;

    /**
     * 会话记忆主键，格式为 userId:conversationId。
     */
    private String memoryId;

    /**
     * 当前会话总轮次。
     */
    private Integer roundCount;

    /**
     * 最后一轮会话内容，供前端展示摘要。
     */
    private ChatRoundResp lastRound;

    /**
     * 当前会话完整历史记录。
     */
    private List<ChatRoundResp> history;
}
