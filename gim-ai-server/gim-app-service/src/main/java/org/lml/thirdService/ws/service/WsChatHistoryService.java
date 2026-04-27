package org.lml.thirdService.ws.service;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.lml.thirdService.ws.entity.resp.ChatHistorySessionResp;
import org.lml.thirdService.ws.entity.resp.ChatRoundResp;
import org.lml.thirdService.ws.memory.RedisChatMemoryStore;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * WebSocket 会话历史查询服务。
 * 负责扫描当前登录用户在 Redis 中的全部聊天会话，并组装成前端可直接恢复的结果。
 */
@Slf4j
@Service
public class WsChatHistoryService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * 查询当前登录用户的全部会话历史。
     */
    public List<ChatHistorySessionResp> queryCurrentUserSessions() {
        String userId = String.valueOf(StpUtil.getLoginId());
        String userPrefix = redisChatMemoryStore.buildUserRedisKeyPrefix(userId);

        log.info("开始查询当前用户的会话历史，userId={}，redis前缀={}", userId, userPrefix);

        List<String> redisKeys = scanConversationKeys(userPrefix + "*");
        if (redisKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatHistorySessionResp> responseList = new ArrayList<>();
        for (String redisKey : redisKeys) {
            try {
                String memoryId = redisChatMemoryStore.extractMemoryId(redisKey);
                String conversationId = redisChatMemoryStore.extractConversationId(userId, redisKey);
                List<ChatRoundResp> history = redisChatMemoryStore.getConversationRounds(memoryId);

                ChatHistorySessionResp response = new ChatHistorySessionResp();
                response.setConversationId(conversationId);
                response.setMemoryId(memoryId);
                response.setHistory(history);
                response.setRoundCount(history.size());
                response.setLastRound(history.isEmpty() ? null : history.get(history.size() - 1));
                responseList.add(response);
            } catch (Exception exception) {
                log.error("解析单个会话历史失败，redisKey={}，原因={}", redisKey, exception.getMessage(), exception);
            }
        }

        responseList.sort(Comparator.comparing(ChatHistorySessionResp::getConversationId).reversed());
        return responseList;
    }

    /**
     * 使用 SCAN 查询当前用户的全部会话 key，避免使用全量 KEYS。
     */
    private List<String> scanConversationKeys(String pattern) {
        List<String> redisKeys = stringRedisTemplate.execute((RedisCallback<List<String>>) connection -> doScan(connection, pattern));
        return redisKeys == null ? Collections.emptyList() : redisKeys;
    }

    /**
     * 执行 Redis SCAN 游标遍历。
     */
    private List<String> doScan(RedisConnection connection, String pattern) {
        List<String> redisKeys = new ArrayList<>();
        Cursor<byte[]> cursor = null;
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build();
            cursor = connection.scan(scanOptions);
            while (cursor.hasNext()) {
                redisKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (DataAccessException exception) {
            log.error("扫描 Redis 会话 key 失败，pattern={}，原因={}", pattern, exception.getMessage(), exception);
            throw new IllegalStateException("扫描会话 key 失败", exception);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception exception) {
                    log.warn("关闭 Redis SCAN 游标失败，pattern={}，原因={}", pattern, exception.getMessage());
                }
            }
        }
        return redisKeys;
    }
}
