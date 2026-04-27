package org.lml.thirdService.ws.config;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * WebSocket 握手认证拦截器。
 * 当前项目普通 HTTP 请求通过 Authorization 头传递 token，浏览器原生 WebSocket 无法复用该方式，
 * 因此这里单独使用 URL query 参数中的 token 完成握手认证。
 */
@Slf4j
@Component
public class AiAuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final String WEBSOCKET_USER_ID = "WEBSOCKET_USER_ID";

    /**
     * 在握手阶段校验 token，并把可信用户ID写入 WebSocket Session 属性。
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.info("WebSocket 开始握手认证");

        if (!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            log.warn("WebSocket 握手拒绝：无法解析 HttpServletRequest");
            return false;
        }

        HttpServletRequest httpRequest = servletServerHttpRequest.getServletRequest();
        String token = resolveToken(httpRequest);
        log.info("WebSocket 握手获取到的 token={}", token);

        if (StrUtil.isBlank(token)) {
            log.warn("WebSocket 握手拒绝：缺少 token 参数");
            return false;
        }

        try {
            // 直接调用 Sa-Token 提供的 token 反查能力，避免依赖项目里未维护的自定义 Redis 键结构。
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                log.warn("WebSocket 握手拒绝：token 已过期或不存在");
                return false;
            }

            // 握手成功后仅在 Session 属性中保存可信 userId，供后续聊天链路构造 memoryId。
            String userId = String.valueOf(loginId);
            attributes.put(WEBSOCKET_USER_ID, userId);

            log.info("WebSocket 握手认证成功，userId={}", userId);
            return true;
        } catch (Exception exception) {
            log.error("WebSocket 握手认证异常", exception);
            return false;
        }
    }

    /**
     * 从握手请求中解析 URL query 参数里的 token。
     * 前端需要使用原始 token 建立连接，不传 Bearer 前缀。
     */
    private String resolveToken(HttpServletRequest request) {
        return request.getParameter("token");
    }

    /**
     * 握手完成后的扩展入口。
     * 当前无需额外处理，保留空实现。
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
