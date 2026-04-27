package org.lml.thirdService.ws.config;

import lombok.extern.slf4j.Slf4j;
import org.lml.thirdService.ws.handle.VideoWebSocketHandler;
import org.lml.thirdService.ws.handle.WebRTCSignalHandler;
import org.lml.thirdService.ws.handle.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final VideoWebSocketHandler videoWebSocketHandler;

    private final WebRTCSignalHandler webRTCSignalHandler;

    private final AiAuthHandshakeInterceptor aiAuthHandshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           VideoWebSocketHandler videoWebSocketHandler,
                           WebRTCSignalHandler webRTCSignalHandler,
                           AiAuthHandshakeInterceptor aiAuthHandshakeInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.videoWebSocketHandler = videoWebSocketHandler;
        this.webRTCSignalHandler = webRTCSignalHandler;
        this.aiAuthHandshakeInterceptor = aiAuthHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // AI: 助手处理模型
        registry.addHandler(chatWebSocketHandler, "/chat-ws")
                .setAllowedOrigins("*")
                .addInterceptors(aiAuthHandshakeInterceptor);

        registry.addHandler(videoWebSocketHandler, "/video")
                .setAllowedOrigins("*")
                .addInterceptors(aiAuthHandshakeInterceptor);

        // 2️⃣ WebRTC 在线铜线Handle
        registry.addHandler(webRTCSignalHandler, "/webrtc")
                .setAllowedOrigins("*");
    }

}
