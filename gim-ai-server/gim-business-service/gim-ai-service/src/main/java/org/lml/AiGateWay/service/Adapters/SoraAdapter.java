package org.lml.AiGateWay.service.Adapters;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.common.BaseApiClient;
import org.lml.AiGateWay.enums.VideoProvider;
import org.lml.AiGateWay.req.GatewayRequest;
import org.lml.AiGateWay.service.VideoStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Sora 视频生成适配器。
 */
@Slf4j
@Service
public class SoraAdapter extends BaseApiClient implements VideoStrategy {

    @Value("${api.sora.url:https://api.sora2api.ai}")
    private String baseUrl;

    @Value("${api.sora.key:111}")
    private String apiKey;

    @Value("${api.sora.callback-url:https://api.example.com/callback}")
    private String callbackUrl;

    /**
     * 返回当前策略编码。
     */
    @Override
    public String getBizType() {
        return VideoProvider.SORA_TO_VIDEO.getCode();
    }

    /**
     * 提交 Sora 创建任务。
     */
    @Override
    public ApiResult<String> execute(GatewayRequest gatewayRequest) {
        if (gatewayRequest == null || ObjectUtil.isNull(gatewayRequest.getParams())) {
            return ApiResult.taskFail("请求失败: 请求参数为空");
        }

        Map<String, Object> params = gatewayRequest.getParams();
        String prompt = MapUtil.getStr(params, "prompt");
        String fileUrl = MapUtil.getStr(params, "fileUrl");
        if (StrUtil.isBlank(prompt)) {
            return ApiResult.taskFail("非法请求: 提示词不能为空");
        }
        if (StrUtil.isBlank(fileUrl)) {
            return ApiResult.taskFail("非法请求: 文件地址不能为空");
        }

        Map<String, String> headers = buildCommonHeaders();
        Map<String, Object> body = buildGenerateBody(prompt, fileUrl);
        log.info("Sora 开始提交任务, prompt={}", prompt);
        return request(baseUrl, "/api/v1/sora2api/generate", "POST", headers, body, this::parseGenerateResponse);
    }

    /**
     * 查询 Sora 任务结果。
     */
    @Override
    public ApiResult<String> query(GatewayRequest gatewayRequest) {
        if (gatewayRequest == null || ObjectUtil.isNull(gatewayRequest.getParams())) {
            return ApiResult.taskFail("请求失败: 请求参数为空");
        }

        String taskId = MapUtil.getStr(gatewayRequest.getParams(), "taskId");
        if (StrUtil.isBlank(taskId)) {
            return ApiResult.taskFail("请求失败: taskId 不能为空");
        }

        Map<String, String> headers = buildCommonHeaders();
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("taskId", taskId);
        return request(baseUrl, "/api/v1/sora2api/record-info", "GET", headers, queryParams, this::parseQueryResponse);
    }

    /**
     * 构建公共请求头。
     */
    private Map<String, String> buildCommonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        return headers;
    }

    /**
     * 构建 Sora 创建请求体。
     */
    private Map<String, Object> buildGenerateBody(String prompt, String fileUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        body.put("aspectRatio", "landscape");
        body.put("quality", "hd");
        body.put("watermark", "lml");
        body.put("callBackUrl", callbackUrl);
        body.put("imageUrls", fileUrl);
        return body;
    }

    /**
     * 解析 Sora 创建任务响应。
     */
    private ApiResult<String> parseGenerateResponse(JSONObject json) {
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            String msg = json.getStr("message", "接口未返回有效数据");
            log.error("Sora 创建任务返回异常, body={}", json.toString());
            return ApiResult.taskFail("接口异常: " + msg);
        }

        String taskId = data.getStr("taskId");
        if (StrUtil.isBlank(taskId)) {
            log.error("Sora 创建任务缺少 taskId, body={}", data.toString());
            return ApiResult.taskFail("解析失败: 未获取到任务ID");
        }

        return ApiResult.success(taskId);
    }

    /**
     * 解析 Sora 查询响应。
     */
    private ApiResult<String> parseQueryResponse(JSONObject json) {
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            return ApiResult.taskFail("Sora 查询返回数据为空");
        }

        Integer successFlag = data.getInt("successFlag");
        JSONObject responseObj = data.getJSONObject("response");
        String taskIdReturned = data.getStr("taskId");

        if (Integer.valueOf(1).equals(successFlag)) {
            String videoUrl = responseObj == null ? null : responseObj.getStr("imageUrl");
            log.info("Sora 任务已完成, taskId={}, url={}", taskIdReturned, videoUrl);
            return ApiResult.success(videoUrl);
        }
        if (Integer.valueOf(0).equals(successFlag)) {
            return ApiResult.processing(taskIdReturned, "Sora 任务正在处理中");
        }
        if (Integer.valueOf(2).equals(successFlag)) {
            return ApiResult.error(String.valueOf(successFlag), "Sora 任务生成失败");
        }
        if (Integer.valueOf(3).equals(successFlag)) {
            return ApiResult.error(String.valueOf(successFlag), "Sora 任务创建成功但生成失败");
        }
        return ApiResult.taskFail("Sora 返回了未知状态");
    }
}
