package org.lml.config.sensitive;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lml.config.resp.GatewayResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sentinel 网关限流配置类。
 * 负责定义网关路由限流、API 分组限流以及自定义限流拦截逻辑。
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class GatewaySentinelConfiguration {

    // 路由 ID 与 API 名称定义
    private static final String HTTP_ROUTE_ID = "gim-service-http-route";
    private static final String CHAT_WS_ROUTE_ID = "gim-chat-ws-route";
    private static final String VIDEO_WS_ROUTE_ID = "gim-video-ws-route";
    private static final String LOGIN_API_NAME = "login-api";
    private static final String AI_VIDEO_API_NAME = "ai-video-api";
    private static final String TEST_API_NAME = "test-api";

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;
    private final GatewayRateLimitProperties gatewayRateLimitProperties;
    private final GatewayResponseWriter gatewayResponseWriter;

    /**
     * 初始化 Sentinel 网关规则。
     * @PostConstruct 确保在 Bean 初始化完成之后加载规则配置。
     */
    @PostConstruct
    public void initGatewayRules() {
        // 1. 加载 API 分组定义
        GatewayApiDefinitionManager.loadApiDefinitions(buildApiDefinitions());
        // 2. 加载对应的限流规则
        GatewayRuleManager.loadRules(buildGatewayFlowRules());

        // 3. 配置自定义限流处理器（当触发限流时返回统一的 JSON 格式）
        BlockRequestHandler blockRequestHandler = (exchange, throwable) -> ServerResponse
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(
                        gatewayResponseWriter.buildBody(429, "请求过于频繁，请稍后再试"))
                );

        // 设置全局限流回调
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
        log.info("Sentinel 网关限流规则及全局回调初始化完成");
    }

    /**
     * 注册 Sentinel 网关过滤器。
     * 该过滤器会拦截所有的网关请求并进行资源匹配与限流检查。
     */
    @Bean
    public SentinelGatewayFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 注册 Sentinel 网关异常处理器。
     * Order 设置为 HIGHEST_PRECEDENCE 确保在其他 WebFlux 异常处理器之前拦截限流异常。
     */
    @Bean
    @org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * 构建自定义 API 分组映射。
     * 通过定义通配符匹配规则将一组 URL 归纳为一个资源名。
     */
    private Set<ApiDefinition> buildApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();
        definitions.add(new ApiDefinition(LOGIN_API_NAME)
                .setPredicateItems(Collections.singleton(new ApiPathPredicateItem().setPattern("/user/doLogin"))));
        definitions.add(new ApiDefinition(AI_VIDEO_API_NAME)
                .setPredicateItems(Collections.singleton(new ApiPathPredicateItem().setPattern("/ai/video/**"))));
        definitions.add(new ApiDefinition(TEST_API_NAME)
                .setPredicateItems(Collections.singleton(new ApiPathPredicateItem().setPattern("/api/**"))));
        return definitions;
    }

    /**
     * 聚合路由级别限流规则与 API 分组级别限流规则。
     */
    private Set<GatewayFlowRule> buildGatewayFlowRules() {
        return new HashSet<>(Arrays.asList(
                routeRule(HTTP_ROUTE_ID, gatewayRateLimitProperties.getDefaultCount(), gatewayRateLimitProperties.getDefaultIntervalSeconds()),
                routeRule(CHAT_WS_ROUTE_ID, gatewayRateLimitProperties.getChatWsCount(), gatewayRateLimitProperties.getChatWsIntervalSeconds()),
                routeRule(VIDEO_WS_ROUTE_ID, gatewayRateLimitProperties.getVideoWsCount(), gatewayRateLimitProperties.getVideoWsIntervalSeconds()),
                apiRule(LOGIN_API_NAME, gatewayRateLimitProperties.getLoginCount(), gatewayRateLimitProperties.getLoginIntervalSeconds()),
                apiRule(AI_VIDEO_API_NAME, gatewayRateLimitProperties.getAiVideoCount(), gatewayRateLimitProperties.getAiVideoIntervalSeconds()),
                apiRule(TEST_API_NAME, gatewayRateLimitProperties.getApiCount(), gatewayRateLimitProperties.getApiIntervalSeconds())
        ));
    }

    /**
     * 创建基于路由 ID 的限流规则。
     */
    private GatewayFlowRule routeRule(String routeId, double count, int intervalSeconds) {
        return new GatewayFlowRule(routeId)
                .setCount(count)
                .setIntervalSec(intervalSeconds);
    }

    /**
     * 创建基于自定义 API 分组的限流规则。
     * 必须指定 RESOURCE_MODE_CUSTOM_API_NAME 才能正确匹配 API 分组。
     */
    private GatewayFlowRule apiRule(String apiName, double count, int intervalSeconds) {
        return new GatewayFlowRule(apiName)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(count)
                .setIntervalSec(intervalSeconds);
    }
}