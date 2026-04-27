package org.lml.thirdService.ws.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * WebSocket 聊天会话记忆提供器。
 * 按会话主键为每个聊天会话创建独立的消息窗口，并绑定 Redis 持久化存储。
 */
@Component
public class WsChatMemoryProvider implements ChatMemoryProvider {

    private static final int MAX_MESSAGES = 50;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 为指定会话创建 LangChain4j 记忆对象。
     */
    @Override
    public ChatMemory get(Object memoryId) {
        // 这里统一使用消息窗口模型，并把存储后端绑定到 Redis。
        // 这样 LangChain4j 在处理同一个 memoryId 时，会自动加载旧消息并追加新消息。
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }
}
