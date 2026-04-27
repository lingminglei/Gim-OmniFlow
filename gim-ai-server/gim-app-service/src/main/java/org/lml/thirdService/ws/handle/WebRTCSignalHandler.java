// WebRTCSignalHandler.java

package org.lml.thirdService.ws.handle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebRTCSignalHandler extends TextWebSocketHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TYPE_USER_LIST = "user_list";

    /**
     * userId -> session (存储在线用户)
     */
    private static final Map<String, WebSocketSession> USERS =
            new ConcurrentHashMap<>();

    /**
     * 辅助方法：发送在线用户列表给所有用户
     */
    private void broadcastUserList() {
        // 提取当前所有的在线用户ID
        List<String> userIds = new ArrayList<>(USERS.keySet());

        try {
            // 构建要发送的 JSON 消息
            String message = MAPPER.writeValueAsString(Map.of(
                    "type", TYPE_USER_LIST,
                    "data", userIds
            ));

            TextMessage textMessage = new TextMessage(message);

            // 遍历所有 Session 并发送
            for (WebSocketSession session : USERS.values()) {
                if (session.isOpen()) {
                    // ⚠️ 注意：这里直接发送的是所有用户ID，前端会自行过滤掉自己
                    session.sendMessage(textMessage);
                }
            }
            log.info("📢 广播在线用户列表: {}", userIds);

        } catch (IOException e) {
            log.error("广播用户列表失败", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("📡 WebRTC 信令连接建立 sessionId={}", session.getId());
        // 客户端连接后，等待其发送 'register' 消息来注册 ID
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) throws Exception {

        JsonNode json = MAPPER.readTree(message.getPayload());

        String type = json.get("type").asText();
        // 客户端在 'register' 消息中发送 ID
        String from = json.has("from") ? json.get("from").asText() : null;
        String to = json.has("to") ? json.get("to").asText() : null;


        // 1. 用户注册
        if ("register".equals(type) && from != null) {
            boolean isNewUser = USERS.putIfAbsent(from, session) == null;
            if (isNewUser) {
                log.info("✅ 用户注册成功: userId={}", from);
                broadcastUserList(); // 新用户上线，广播更新后的列表
            }
            return; // 注册消息处理完毕，不进行转发
        }

        // 2. 信令转发 (offer/answer/candidate)
        if (from == null || to == null) {
            log.warn("⚠️ 收到信令格式不完整，from或to为空");
            return;
        }

        log.info("📨 信令 type={}, from={}, to={}", type, from, to);

        WebSocketSession target = USERS.get(to);
        if (target != null && target.isOpen()) {
            target.sendMessage(message);
        } else {
            log.warn("⚠️ 目标用户不在线 to={}", to);
            // 💡 可以在此发送一个 'error' 信令给发起方
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        // 移除断开连接的用户
        USERS.entrySet().removeIf(e -> e.getValue().equals(session));

        // 用户断开，广播更新后的列表
        broadcastUserList();

        log.info("❌ WebRTC 信令断开 sessionId={}, code={}",
                session.getId(), status.getCode());
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception) {

        log.error("🔥 WebRTC 信令异常 sessionId={}",
                session.getId(), exception);

        // 异常处理：移除并广播列表
        USERS.entrySet().removeIf(e -> e.getValue().equals(session));
        broadcastUserList();
    }
}
