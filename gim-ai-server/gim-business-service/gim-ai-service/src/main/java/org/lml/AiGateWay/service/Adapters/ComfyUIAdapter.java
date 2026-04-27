package org.lml.AiGateWay.service.Adapters;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.common.BaseApiClient;
import org.lml.AiGateWay.config.WorkflowResourceReader;
import org.lml.AiGateWay.enums.VideoProvider;
import org.lml.AiGateWay.req.GatewayRequest;
import org.lml.AiGateWay.service.VideoStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ComfyUI 文生图适配器。
 */
@Slf4j
@Service
public class ComfyUIAdapter extends BaseApiClient implements VideoStrategy {

    @Value("${api.comfy.url:http://127.0.0.1:8000}")
    private String baseUrl;

    private final WorkflowResourceReader reader;

    public ComfyUIAdapter(WorkflowResourceReader reader) {
        this.reader = reader;
    }

    /**
     * 返回当前策略编码。
     */
    @Override
    public String getBizType() {
        return VideoProvider.COMFY_UI.getCode();
    }

    /**
     * 提交 ComfyUI 创建任务。
     */
    @Override
    public ApiResult<String> execute(GatewayRequest gatewayRequest) {
        if (gatewayRequest == null || ObjectUtil.isNull(gatewayRequest.getParams())) {
            return ApiResult.taskFail("请求失败: 请求参数为空");
        }

        Map<String, Object> params = gatewayRequest.getParams();
        String prompt = MapUtil.getStr(params, "prompt");
        if (StrUtil.isBlank(prompt)) {
            return ApiResult.taskFail("非法请求: 提示词不能为空");
        }

        String baseJson = reader.readWorkflow("textToImage.json");
        Map<String, Object> payload = buildComfyPayload(baseJson, prompt);
        log.info("ComfyUI 开始提交任务, prompt={}", prompt);
        return request(baseUrl, "/prompt", "POST", new HashMap<>(), payload, this::parseSubmitResponse);
    }

    /**
     * 查询 ComfyUI 任务结果。
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

        return request(baseUrl, "/history/" + taskId, "GET", new HashMap<>(), null,
                json -> parseQueryResponse(json, taskId));
    }

    /**
     * 按照 ComfyUI 工作流格式组装请求体。
     */
    private Map<String, Object> buildComfyPayload(String baseJson, String prompt) {
        JSONObject workflow = cn.hutool.json.JSONUtil.parseObj(baseJson);

        JSONObject node45 = workflow.getJSONObject("45");
        if (node45 != null) {
            node45.getJSONObject("inputs").set("text", prompt);
        }

        JSONObject node44 = workflow.getJSONObject("44");
        if (node44 != null) {
            long newSeed = ThreadLocalRandom.current().nextLong(100000000000000L, 999999999999999L);
            node44.getJSONObject("inputs").set("seed", newSeed);
        }

        JSONObject node9 = workflow.getJSONObject("9");
        if (node9 != null) {
            node9.getJSONObject("inputs").set("filename_prefix", "Gateway_" + System.currentTimeMillis());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", workflow);
        payload.put("client_id", "gateway_backend");
        return payload;
    }

    /**
     * 解析 ComfyUI 提交任务响应。
     */
    private ApiResult<String> parseSubmitResponse(JSONObject json) {
        String promptId = json.getStr("prompt_id");
        if (StringUtils.isNotBlank(promptId)) {
            return ApiResult.success(promptId);
        }

        String errorMsg = json.getStr("error", "未知错误");
        return ApiResult.taskFail("ComfyUI 提交失败: " + errorMsg);
    }

    /**
     * 解析 ComfyUI 查询结果。
     */
    private ApiResult<String> parseQueryResponse(JSONObject json, String promptId) {
        if (!json.containsKey(promptId)) {
            return ApiResult.processing(promptId, "ComfyUI 正在生成中");
        }

        try {
            Object value = json.getByPath(promptId + ".outputs.9.images[0].filename");
            String filename = value == null ? null : String.valueOf(value);
            if (StringUtils.isNotBlank(filename)) {
                String url = baseUrl + "/view?filename=" + filename;
                return ApiResult.success(url);
            }
            return ApiResult.taskFail("ComfyUI 未能生成结果文件");
        } catch (Exception e) {
            log.error("解析 ComfyUI 结果异常, promptId={}", promptId, e);
            return ApiResult.error("500", "解析 ComfyUI 报文失败: " + e.getMessage());
        }
    }
}
