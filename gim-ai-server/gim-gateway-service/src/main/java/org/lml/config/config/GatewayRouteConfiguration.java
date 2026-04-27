package org.lml.config.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置。
 */
@Configuration
public class GatewayRouteConfiguration {

    @Value("${gateway.route.service-id:gim-app-service}")
    private String serviceId;

    /**
     * 统一声明 HTTP 与 WebSocket 路由。
     *
     * @param builder 路由构建器
     * @return 路由定位器
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        String wsUri = "lb:ws://" + serviceId;
        return builder.routes()
                .route("gim-service-http-route", route -> route
                        .path("/api/**",
                                "/user/**",
                                "/pay/**",
                                "/ai/**",
                                "/commonApi/**",
                                "/file/**",
                                "/filetransfer/**",
                                "/upload/**",
                                "/knowledge/**",
                                "/canvasData/**",
                                "/news/**",
                                "/gim/**",
                                "/recoveryFile/**")
                        .uri("lb://gim-app-service"))
                .route("gim-chat-ws-route", route -> route.path("/chat-ws").uri(wsUri))
                .route("gim-video-ws-route", route -> route.path("/video").uri(wsUri))
                .build();
    }
}
