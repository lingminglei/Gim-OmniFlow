package org.lml.AiGateWay.common;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.lml.AiGateWay.common.ApiResult;

import java.util.Map;
import java.util.function.Function;

/**
 * 第三方 HTTP 调用基础类。
 */
@Slf4j
public abstract class BaseApiClient {

    /**
     * 统一执行 HTTP 请求并交给具体适配器解析返回值。
     */
    protected <T> ApiResult<T> request(String baseUrl, String path, String method,
                                       Map<String, String> headers, Object body,
                                       Function<JSONObject, ApiResult<T>> parser) {
        String url = baseUrl + path;
        try {
            HttpResponse response = executeRequest(url, method, headers, body);
            if (response.getStatus() != 200) {
                return ApiResult.error(String.valueOf(response.getStatus()), "接口响应异常: " + response.body());
            }

            String bodyStr = response.body();
            if (StrUtil.isBlank(bodyStr)) {
                return ApiResult.error("500", "接口返回内容为空");
            }

            JSONObject json = JSONUtil.parseObj(bodyStr);
            return parser.apply(json);
        } catch (Exception e) {
            log.error("调用第三方接口发生异常, url={}", url, e);
            return ApiResult.error("500", "系统异常: " + e.getMessage());
        }
    }

    /**
     * 统一处理 GET 和非 GET 请求的参数拼接与请求发送。
     */
    private HttpResponse executeRequest(String url, String method, Map<String, String> headers, Object body) {
        boolean isGet = "GET".equalsIgnoreCase(method);

        if (isGet && body != null) {
            Map<String, Object> params = body instanceof Map ? (Map<String, Object>) body : BeanUtil.beanToMap(body);
            url = HttpUtil.urlWithForm(url, params, CharsetUtil.CHARSET_UTF_8, false);
        }

        HttpRequest request = HttpUtil.createRequest(Method.valueOf(method), url);
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (StrUtil.isNotBlank(value)) {
                    request.header(key, value.trim());
                }
            });
        }

        if (!isGet && body != null) {
            request.body(JSONUtil.toJsonStr(body));
        } else if (isGet) {
            request.removeHeader(Header.CONTENT_TYPE);
        }

        log.info("开始调用第三方接口, method={}, url={}", method, url);
        return request.execute();
    }
}
