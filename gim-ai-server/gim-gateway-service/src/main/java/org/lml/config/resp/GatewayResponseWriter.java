package org.lml.config.resp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.result.CommonResult;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayResponseWriter {

    private final ObjectMapper objectMapper;

    public Mono<Void> write(ServerHttpResponse response, HttpStatus httpStatus, int code, String message) {
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        CommonResult<Object> result = CommonResult.errorResponse(message, code);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":" + code + ",\"data\":null,\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            log.error("Failed to serialize gateway response body", e);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public CommonResult<Object> buildBody(int code, String message) {
        return CommonResult.errorResponse(message, code);
    }
}
