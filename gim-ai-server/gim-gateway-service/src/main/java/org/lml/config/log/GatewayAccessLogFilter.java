package org.lml.config.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 网关访问日志过滤器。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayAccessLogFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final GatewayLogProperties gatewayLogProperties;

    /**
     * 统一记录请求与响应日志。
     *
     * @param exchange 当前请求上下文
     * @param chain 过滤器链
     * @return 过滤结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = resolveRequestId(request);
        long startTime = System.currentTimeMillis();
        String requestTime = LocalDateTime.now().toString();

        if (shouldLogRequestBody(request)) {
            return DataBufferUtils.join(request.getBody())
                    .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                    .flatMap(dataBuffer -> {
                        byte[] bodyBytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bodyBytes);
                        DataBufferUtils.release(dataBuffer);

                        String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);
                        logRequest(request, requestId, requestTime, requestBody);

                        // 请求体被读取后要重新包装，否则下游业务无法再次读取。
                        Flux<DataBuffer> cachedBody = Flux.defer(() ->
                                Mono.just(exchange.getResponse().bufferFactory().wrap(bodyBytes)));

                        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(addRequestIdHeader(request, requestId)) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return cachedBody;
                            }
                        };

                        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
                        return chain.filter(exchange.mutate().request(decoratedRequest).build())
                                .doFinally(signalType -> logResponse(exchange, requestId, startTime));
                    });
        }

        logRequest(request, requestId, requestTime, null);
        ServerHttpRequest requestWithId = addRequestIdHeader(request, requestId);
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        return chain.filter(exchange.mutate().request(requestWithId).build())
                .doFinally(signalType -> logResponse(exchange, requestId, startTime));
    }

    /**
     * 指定日志过滤器的执行顺序。
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 判断当前请求是否适合记录请求体。
     *
     * @param request 当前请求
     * @return 是否记录请求体
     */
    private boolean shouldLogRequestBody(ServerHttpRequest request) {
        MediaType contentType = request.getHeaders().getContentType();
        if (contentType == null) {
            return false;
        }
        if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)
                || MediaType.APPLICATION_OCTET_STREAM.isCompatibleWith(contentType)) {
            return false;
        }
        return MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType);
    }

    /**
     * 记录请求日志。
     *
     * @param request 当前请求
     * @param requestId 请求唯一标识
     * @param requestTime 请求进入时间
     * @param requestBody 请求体
     */
    private void logRequest(ServerHttpRequest request, String requestId, String requestTime, String requestBody) {
        URI uri = request.getURI();
        String query = uri.getRawQuery();
        String finalBody = trimBody(requestBody);
        log.info("网关接收请求, requestId={}, method={}, api={}, query={}, body={}, requestTime={}",
                requestId,
                request.getMethodValue(),
                uri.getPath(),
                StringUtils.hasText(query) ? query : "",
                finalBody,
                requestTime);
    }

    /**
     * 记录响应日志。
     *
     * @param exchange 当前请求上下文
     * @param requestId 请求唯一标识
     * @param startTime 请求开始时间
     */
    private void logResponse(ServerWebExchange exchange, String requestId, long startTime) {
        long cost = System.currentTimeMillis() - startTime;
        Integer statusCode = exchange.getResponse().getRawStatusCode();
        log.info("网关请求完成, requestId={}, status={}, cost={}ms", requestId, statusCode, cost);
    }

    /**
     * 生成或复用请求唯一标识。
     *
     * @param request 当前请求
     * @return 请求唯一标识
     */
    private String resolveRequestId(ServerHttpRequest request) {
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        return StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
    }

    /**
     * 给转发请求补充统一请求标识。
     *
     * @param request 原始请求
     * @param requestId 请求唯一标识
     * @return 补充请求头后的请求对象
     */
    private ServerHttpRequest addRequestIdHeader(ServerHttpRequest request, String requestId) {
        return request.mutate().headers(headers -> headers.set(REQUEST_ID_HEADER, requestId)).build();
    }

    /**
     * 控制请求体日志长度，避免日志过大。
     *
     * @param requestBody 原始请求体
     * @return 截断后的请求体
     */
    private String trimBody(String requestBody) {
        if (!StringUtils.hasText(requestBody)) {
            return "";
        }
        int maxLength = gatewayLogProperties.getBodyMaxLength();
        if (requestBody.length() <= maxLength) {
            return requestBody;
        }
        return requestBody.substring(0, maxLength) + "...(已截断)";
    }
}
