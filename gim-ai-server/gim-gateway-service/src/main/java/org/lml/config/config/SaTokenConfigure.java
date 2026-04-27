package org.lml.config.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.StopMatchException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.lml.config.resp.GatewayResponseWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class SaTokenConfigure {

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/commonApi/captcha",
            "/user/doLogin",
            "/chat-ws",
            "/video",
            "/webrtc",
            "/filetransfer/preview",
            "/ai/video/createPrompt",
            "/ai/video/createVideo",
            "/ai/video/queryResult",
            "/api/**",
            "/chat/aiAsk",
            "/api/queryState",
            "/user/logout",
            "/upload/**",
            "/file/upload"
    );

    @Bean
    public WebFilter getSaReactorFilter(GatewayResponseWriter gatewayResponseWriter) {
        return (exchange, chain) -> {
            exchange.getAttributes().put("WEB_FILTER_CHAIN_KEY", chain);
            try {
                SaReactorSyncHolder.setContext(exchange);
                SaRouter.match("/**")
                        .notMatch(EXCLUDE_PATHS)
                        .check(r -> StpUtil.checkLogin());
                SaReactorSyncHolder.clearContext();
            } catch (StopMatchException e) {
                SaReactorSyncHolder.clearContext();
            } catch (Throwable e) {
                SaReactorSyncHolder.clearContext();
                return writeAuthError(exchange, gatewayResponseWriter, e);
            }

            SaReactorSyncHolder.setContext(exchange);
            return chain.filter(exchange)
                    .subscriberContext(ctx -> ctx.put(ServerWebExchange.class, exchange))
                    .doFinally(signalType -> SaReactorSyncHolder.clearContext());
        };
    }

    private Mono<Void> writeAuthError(ServerWebExchange exchange,
                                      GatewayResponseWriter gatewayResponseWriter,
                                      Throwable e) {
        log.warn("Gateway auth failed, path={}, tokenSummary={}, exceptionType={}, exceptionMessage={}",
                exchange.getRequest().getPath(),
                getTokenSummary(exchange),
                e.getClass().getSimpleName(),
                e.getMessage());
        return gatewayResponseWriter.write(
                exchange.getResponse(),
                HttpStatus.UNAUTHORIZED,
                401,
                buildErrorMessage(e)
        );
    }

    private String buildErrorMessage(Throwable e) {
        if (e instanceof NotLoginException) {
            return e.getMessage();
        }
        return "网关鉴权失败";
    }

    private String getTokenSummary(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst("satoken");
        if (token == null || token.trim().isEmpty()) {
            token = exchange.getRequest().getQueryParams().getFirst("token");
        }
        if (token == null || token.trim().isEmpty()) {
            return "none";
        }
        if (token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
