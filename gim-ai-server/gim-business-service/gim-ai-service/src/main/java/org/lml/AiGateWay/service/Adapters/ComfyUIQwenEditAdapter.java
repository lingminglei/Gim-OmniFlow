package org.lml.AiGateWay.service.Adapters;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ComfyUI 图像编辑适配器。
 */
@Slf4j
@Service
public class ComfyUIQwenEditAdapter extends BaseApiClient implements VideoStrategy {

    @Value("${api.comfy.url:http://127.0.0.1:8000}")
    private String baseUrl;

    private final WorkflowResourceReader reader;

    public ComfyUIQwenEditAdapter(WorkflowResourceReader reader) {
        this.reader = reader;
    }

    /**
     * 返回当前策略编码。
     */
    @Override
    public String getBizType() {
        return VideoProvider.COMFY_UI_IMAGE_EDIT.getCode();
    }

    /**
     * 提交 ComfyUI 图像编辑任务。
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

        String comfyFileName = handleImageUpload(fileUrl);
        String baseJson = reader.readWorkflow("qwen_image_edit.json");
        Map<String, Object> payload = buildQwenPayload(baseJson, prompt, comfyFileName);
        log.info("ComfyUI 图像编辑开始提交任务, fileUrl={}", fileUrl);
        return request(baseUrl, "/prompt", "POST", new HashMap<>(), payload, this::parseSubmitResponse);
    }

    /**
     * 查询 ComfyUI 图像编辑任务结果。
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
     * 构建 Qwen 图像编辑工作流请求体。
     */
    private Map<String, Object> buildQwenPayload(String baseJson, String prompt, String comfyFileName) {
        JSONObject root = JSONUtil.parseObj(baseJson);
        JSONObject promptNodes = root.containsKey("prompt") ? root.getJSONObject("prompt") : root;

        if (promptNodes.containsKey("78")) {
            promptNodes.getJSONObject("78").getJSONObject("inputs").set("image", comfyFileName);
        }
        if (promptNodes.containsKey("102:76")) {
            promptNodes.getJSONObject("102:76").getJSONObject("inputs").set("prompt", prompt);
        }
        if (promptNodes.containsKey("102:3")) {
            long newSeed = ThreadLocalRandom.current().nextLong(100000000000000L, 999999999999999L);
            promptNodes.getJSONObject("102:3").getJSONObject("inputs").set("seed", newSeed);
        }
        if (promptNodes.containsKey("60")) {
            promptNodes.getJSONObject("60").getJSONObject("inputs").set("filename_prefix", "QwenEdit_" + System.currentTimeMillis());
        }

        Map<String, Object> finalPayload = new HashMap<>();
        finalPayload.put("client_id", "java_client_" + System.currentTimeMillis());
        finalPayload.put("prompt", promptNodes);
        return finalPayload;
    }

    /**
     * 解析创建任务响应。
     */
    private ApiResult<String> parseSubmitResponse(JSONObject json) {
        String promptId = json.getStr("prompt_id");
        if (StringUtils.isNotBlank(promptId)) {
            return ApiResult.success(promptId);
        }

        String error = json.getStr("error", "提交失败");
        return ApiResult.taskFail("ComfyUI 提交失败: " + error);
    }

    /**
     * 解析查询结果。
     */
    private ApiResult<String> parseQueryResponse(JSONObject json, String promptId) {
        if (!json.containsKey(promptId)) {
            return ApiResult.processing(promptId, "ComfyUI 正在生成中");
        }

        try {
            Object value = json.getByPath(promptId + ".outputs.60.images[0].filename");
            String filename = value == null ? null : String.valueOf(value);
            if (StringUtils.isNotBlank(filename)) {
                String url = baseUrl + "/view?filename=" + filename;
                return ApiResult.success(url);
            }
            return ApiResult.taskFail("ComfyUI 任务完成但未找到输出文件");
        } catch (Exception e) {
            log.error("解析 ComfyUI 图像编辑结果异常, promptId={}", promptId, e);
            return ApiResult.error("500", "解析异常: " + e.getMessage());
        }
    }

    /**
     * 当传入远程地址时，先把图片上传到 ComfyUI。
     */
    private String handleImageUpload(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return "";
        }
        if (!fileUrl.startsWith("http")) {
            return fileUrl;
        }

        log.info("检测到远程图片，开始上传到 ComfyUI, fileUrl={}", fileUrl);
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempFile = HttpUtil.downloadFileFromUrl(fileUrl, tempDir);

        try {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("image", tempFile);
            paramMap.put("overwrite", "true");

            String response = HttpUtil.post(baseUrl + "/upload/image", paramMap);
            JSONObject resJson = JSONUtil.parseObj(response);
            String name = resJson.getStr("name");
            if (StringUtils.isBlank(name)) {
                throw new RuntimeException("ComfyUI 上传接口未返回文件名");
            }
            return name;
        } finally {
            FileUtil.del(tempFile);
        }
    }
}
