package org.lml.thirdService.ws.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * WebSocket 聊天专用的 LangChain4j 接口。
 * 用于承载系统提示词、会话记忆和当前轮问题的统一调用入口。
 */
public interface WsChatAssistant {

    /**
     * 基于指定会话记忆发起流式对话。
     */
    // systemPrompt 作为系统消息排在最前面，用于约束角色和回复风格。
    @SystemMessage("{{systemPrompt}}")
    // promptMessage 承载本轮用户消息，内部已经按“知识库上下文 -> 当前问题”排好顺序。
    @UserMessage("{{promptMessage}}")
    TokenStream chat(@MemoryId String memoryId,
                     @V("systemPrompt") String systemPrompt,
                     @V("promptMessage") String promptMessage);
}
