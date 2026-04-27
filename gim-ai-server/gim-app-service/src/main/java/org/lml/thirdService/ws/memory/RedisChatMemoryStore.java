package org.lml.thirdService.ws.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.thirdService.ws.entity.resp.ChatRoundResp;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 LangChain4j 会话记忆存储实现。
 * Redis 中仅持久化规范化后的对话轮次数据，不保存系统提示词和完整增强提示词。
 */
@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String KEY_PREFIX = "ai_chat_";

    private static final long EXPIRE_DAYS = 7L;

    /**
     * 当前轮用户问题在增强提示词中的固定标记。
     * 如果命中了知识库增强提示词，需要从这个标记后面截取出原始用户问题。
     */
    private static final String CURRENT_QUESTION_MARK = "【当前问题】";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 读取指定会话的历史消息。
     * 优先按新的轮次结构解析；如果命中旧格式，则兼容读取旧版 ChatMessage JSON。
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String redisKey = buildRedisKey(memoryId);

        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isBlank(json)) {
                return Collections.emptyList();
            }

            List<ChatRoundResp> roundMemories = parseRoundMemories(json);
            if (roundMemories != null) {
                return rebuildChatMessages(roundMemories);
            }

            // 兼容读取旧版 Redis 数据，避免上线后老会话全部失效。
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception exception) {
            log.error("从 Redis 读取会话记忆失败，memoryId={}，原因={}", memoryId, exception.getMessage(), exception);
            throw new IllegalStateException("读取会话记忆失败", exception);
        }
    }

    /**
     * 更新指定会话的历史消息，并刷新 Redis 过期时间。
     * 写入 Redis 时只保留用户原始输入、AI 回复内容和轮次编号。
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String redisKey = buildRedisKey(memoryId);

        try {
            List<ChatRoundResp> roundMemories = normalizeRoundMemories(messages);
            String json = OBJECT_MAPPER.writeValueAsString(roundMemories);
            // 写入时同步刷新过期时间，形成“只要会话活跃就续期”的滑动过期效果。
            stringRedisTemplate.opsForValue().set(redisKey, json, EXPIRE_DAYS, TimeUnit.DAYS);
        } catch (Exception exception) {
            log.error("写入 Redis 会话记忆失败，memoryId={}，原因={}", memoryId, exception.getMessage(), exception);
            throw new IllegalStateException("写入会话记忆失败", exception);
        }
    }

    /**
     * 判断指定会话是否已经存在历史记录。
     */
    public boolean hasConversationHistory(String memoryId) {
        String redisKey = buildRedisKey(memoryId);
        Boolean hasKey = stringRedisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(hasKey);
    }

    /**
     * 在会话首次进入时初始化空的历史记录。
     * 这样可以明确区分“新会话已创建”和“Redis 中根本没有这条会话”两种状态。
     */
    public void initializeConversationIfAbsent(String memoryId) {
        String redisKey = buildRedisKey(memoryId);

        try {
            Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "[]", EXPIRE_DAYS, TimeUnit.DAYS);
            if (Boolean.TRUE.equals(created)) {
                log.info("已初始化新的会话历史记录，memoryId={}，redisKey={}", memoryId, redisKey);
            }
        } catch (Exception exception) {
            log.error("初始化 Redis 会话历史记录失败，memoryId={}，原因={}", memoryId, exception.getMessage(), exception);
            throw new IllegalStateException("初始化会话历史记录失败", exception);
        }
    }

    /**
     * 查询指定会话的规范化轮次数据。
     * 返回结果用于前端恢复会话历史。
     */
    public List<ChatRoundResp> getConversationRounds(Object memoryId) {
        String redisKey = buildRedisKey(memoryId);

        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isBlank(json)) {
                return Collections.emptyList();
            }

            List<ChatRoundResp> roundMemories = parseRoundMemories(json);
            if (roundMemories != null) {
                return roundMemories;
            }

            // 如果碰到旧版格式，则先恢复成 ChatMessage，再重新规整成轮次结构返回。
            return normalizeRoundMemories(ChatMessageDeserializer.messagesFromJson(json));
        } catch (Exception exception) {
            log.error("查询会话轮次数据失败，memoryId={}，原因={}", memoryId, exception.getMessage(), exception);
            throw new IllegalStateException("查询会话轮次数据失败", exception);
        }
    }

    /**
     * 构建当前用户的 Redis 扫描前缀。
     */
    public String buildUserRedisKeyPrefix(String userId) {
        return KEY_PREFIX + userId + ":";
    }

    /**
     * 通过 memoryId 生成 Redis key。
     */
    public String buildRedisKey(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }

    /**
     * 从 Redis key 中提取 memoryId。
     */
    public String extractMemoryId(String redisKey) {
        return StringUtils.removeStart(redisKey, KEY_PREFIX);
    }

    /**
     * 从 Redis key 中提取 conversationId。
     */
    public String extractConversationId(String userId, String redisKey) {
        return StringUtils.removeStart(redisKey, buildUserRedisKeyPrefix(userId));
    }

    /**
     * 删除指定会话的历史消息。
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String redisKey = buildRedisKey(memoryId);

        try {
            stringRedisTemplate.delete(redisKey);
        } catch (Exception exception) {
            log.error("删除 Redis 会话记忆失败，memoryId={}，原因={}", memoryId, exception.getMessage(), exception);
            throw new IllegalStateException("删除会话记忆失败", exception);
        }
    }

    /**
     * 将 LangChain4j 的消息列表规整成“轮次结构”。
     * 规范化结构只保留 user、ai、rounds 三个字段。
     */
    private List<ChatRoundResp> normalizeRoundMemories(List<ChatMessage> messages) {
        List<ChatRoundResp> roundMemories = new ArrayList<>();
        ChatRoundResp currentRound = null;
        int roundNumber = 0;

        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                // 系统提示词属于运行时上下文，不进入持久化 Redis。
                continue;
            }

            if (message instanceof UserMessage userMessage) {
                roundNumber++;
                currentRound = new ChatRoundResp();
                currentRound.setRounds(roundNumber);
                // 这里只保留原始用户问题，不保留为了调用模型拼装进去的知识库上下文和系统提示信息。
                currentRound.setUser(extractOriginalUserInput(userMessage.singleText()));
                roundMemories.add(currentRound);
                continue;
            }

            if (message instanceof AiMessage aiMessage) {
                if (currentRound == null) {
                    currentRound = new ChatRoundResp();
                    currentRound.setRounds(++roundNumber);
                    roundMemories.add(currentRound);
                }
                currentRound.setAi(StringUtils.defaultString(aiMessage.text()).trim());
            }
        }

        return roundMemories;
    }

    /**
     * 把 Redis 中的轮次结构还原成 LangChain4j 可识别的消息列表。
     * 这样框架仍然可以按标准 ChatMemory 方式继续拼接历史消息。
     */
    private List<ChatMessage> rebuildChatMessages(List<ChatRoundResp> roundMemories) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (ChatRoundResp roundMemory : roundMemories) {
            if (StringUtils.isNotBlank(roundMemory.getUser())) {
                chatMessages.add(UserMessage.from(roundMemory.getUser()));
            }
            if (StringUtils.isNotBlank(roundMemory.getAi())) {
                chatMessages.add(AiMessage.from(roundMemory.getAi()));
            }
        }
        return chatMessages;
    }

    /**
     * 解析新的轮次结构。
     * 如果当前 Redis 中不是这个结构，返回 null，交给旧格式兼容逻辑处理。
     */
    private List<ChatRoundResp> parseRoundMemories(String json) {
        try {
            List<ChatRoundResp> roundMemories = OBJECT_MAPPER.readValue(
                    json,
                    new TypeReference<List<ChatRoundResp>>() {
                    }
            );

            if (roundMemories == null || roundMemories.isEmpty()) {
                return roundMemories;
            }

            ChatRoundResp first = roundMemories.get(0);
            if (first.getRounds() == null && first.getUser() == null && first.getAi() == null) {
                return null;
            }

            return roundMemories;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 从增强后的用户消息中提取原始用户输入。
     * 这里只去掉“知识库上下文”等系统增强部分，保留真正进入本轮对话的问题内容。
     */
    private String extractOriginalUserInput(String userMessageText) {
        if (StringUtils.isBlank(userMessageText)) {
            return "";
        }

        String normalizedText = userMessageText.trim();
        int currentQuestionIndex = normalizedText.lastIndexOf(CURRENT_QUESTION_MARK);
        if (currentQuestionIndex >= 0) {
            return normalizedText.substring(currentQuestionIndex + CURRENT_QUESTION_MARK.length()).trim();
        }

        return normalizedText;
    }

}
