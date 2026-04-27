package org.lml.thirdService.ws.handle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 处理视频处理帧
 */
@Slf4j
@Component
public class VideoWebSocketHandler extends BinaryWebSocketHandler {

    @Value("${file.path}")
    private String SAVE_PATH;

    @Resource(name = "frameExecutor")
    private Executor executor; // 注入上面定义的线程池


    private static final int MAX_FRAMES = 20;
    // 记录当前已保存的文件路径，方便动态删除
    private final ConcurrentLinkedDeque<String> framePathQueue = new ConcurrentLinkedDeque<>();
    private final AtomicInteger frameCounter = new AtomicInteger(0);

    public VideoWebSocketHandler() {
        // 在组件实例化时直接清空
        String filePaths = SAVE_PATH + "webSocket" +File.separator;
        clearFolder(filePaths);
    }



    /**
     * 保存在线会话
     */
    private static final Set<WebSocketSession> SESSIONS =
            ConcurrentHashMap.newKeySet();

    /**
     * 连接成功
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        session.setBinaryMessageSizeLimit(1024 * 1024); // 1MB
        session.setTextMessageSizeLimit(64 * 1024);

        SESSIONS.add(session);
        log.info("🎥 视频 WS 连接成功 sessionId={}, 在线人数={}",
                session.getId(), SESSIONS.size());
    }

    /**
     * 接收二进制消息（视频帧）
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        // 获取原始 ByteBuffer
        ByteBuffer payload = message.getPayload();

        // 1. 检查数据长度（至少要有8字节的时间戳头）
        if (payload.remaining() < 8) {
            log.warn("⚠️ 收到非法数据包，长度不足8字节");
            return;
        }

        // 2. 解析“对象类型”数据
        // 读取前 8 字节作为时间戳 (DataView.setBigInt64 对应 Java 的 getLong)
        long timestamp = payload.getLong();

        // 剩余的字节全部视为图片文件流
        byte[] imageBytes = new byte[payload.remaining()];
        payload.get(imageBytes);

        log.info("📦 收到有序帧 | Session: {} | 时间戳: {} | 图片大小: {} bytes",
                session.getId(), timestamp, imageBytes.length);

        // 3. 使用 CompletableFuture 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                saveAndCleanFrame(imageBytes, session.getId(), timestamp);
            } catch (IOException e) {
                log.error("❌ 处理视频帧失败: {}", e.getMessage());
            }
        }, executor);
    }

    /**
     * 接收文本消息（控制指令 / 心跳）
     */
    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) {

        String payload = message.getPayload();
        log.info("💬 收到文本消息 sessionId={}, msg={}",
                session.getId(), payload);

        // 示例：简单心跳
        if ("ping".equalsIgnoreCase(payload)) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.error("发送 pong 失败", e);
            }
        }
    }

    /**
     * 连接关闭
     */
    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        SESSIONS.remove(session);
        log.info("❌ 视频 WS 连接关闭 sessionId={}, code={}, 在线人数={}",
                session.getId(), status.getCode(), SESSIONS.size());
    }

    /**
     * 传输异常
     */
    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception) {

        log.error("🔥 WS 连接异常 sessionId={}",
                session.getId(), exception);

        SESSIONS.remove(session);
        try {
            session.close();
        } catch (IOException ignored) {}
    }

    /**
     * 保存当前帧并清理旧帧
     */
    private void saveAndCleanFrame(byte[] data,String sessionId, long timestamp) throws IOException {
        // 确保文件夹存在
        String filePaths = SAVE_PATH + "webSocket" +File.separator + sessionId + File.separator;
        File dir = new File(filePaths);
        if (!dir.exists()) dir.mkdirs();

        // 2. 生成文件名 (使用计数器或时间戳)
        String fileName = filePaths +"frame_" + timestamp  + ".jpg";
        File file = new File(fileName);

        // 3. 写入文件
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }

        // 4. 加入路径队列
        framePathQueue.offer(fileName);
        log.info("💾 已保存帧: {}, 当前队列大小: {}", fileName, framePathQueue.size());

        // 5. 动态删除：如果超过 MAX_FRAMES，则删除最早的那张
        while (framePathQueue.size() > MAX_FRAMES) {
            String oldFilePath = framePathQueue.poll();
            if (oldFilePath != null) {
                File oldFile = new File(oldFilePath);
                if (oldFile.exists()) {
                    boolean deleted = oldFile.delete();
                    log.info("🗑️ 已清理旧帧: {} -> {}", oldFilePath, deleted ? "成功" : "失败");
                }
            }
        }
    }

    /**
     * 清空指定目录下的所有文件
     */
    private void clearFolder(String filePath) {
        try {
            File dir = new File(filePath);
            if (dir.exists()) {
                // 使用 Spring 自带的工具类或 Java 原生 NIO
                FileSystemUtils.deleteRecursively(dir);
                log.info("🧹 已清空旧的缓存目录: {}", filePath);
            }
            // 重新创建空文件夹
            dir.mkdirs();
        } catch (Exception e) {
            log.error("❌ 清空文件夹失败: {}", e.getMessage());
        }
    }
}
