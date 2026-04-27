package org.lml.thirdService.comfyUi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.lml.thirdService.comfyUi.dto.ComfyTaskResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class ComfyUIService {

    private final String COMFY_URL = "http://127.0.0.1:8000";
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * 1. 发起任务 (仅提交，不等待)
     */
    public String submitTask(String baseJson, String prompt, long seed) throws Exception {
        // 修改 JSON 逻辑 (同前)
        JsonNode root = mapper.readTree(baseJson);
        ((ObjectNode) root.get("45").get("inputs")).put("text", prompt);
        ((ObjectNode) root.get("44").get("inputs")).put("seed", seed);

        ObjectNode payload = mapper.createObjectNode();
        payload.set("prompt", root);
        payload.put("client_id", "java_backend");

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(payload),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder().url(COMFY_URL + "/prompt").post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("提交失败: " + response);
            JsonNode res = mapper.readTree(response.body().string());
            return res.get("prompt_id").asText();
        }
    }

    /**
     * 2. 查询任务结果
     */
    public ComfyTaskResponse checkTaskStatus(String promptId) throws IOException {
        // 查询 History 接口
        Request request = new Request.Builder()
                .url(COMFY_URL + "/history/" + promptId)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            JsonNode history = mapper.readTree(response.body().string());

            ComfyTaskResponse taskRes = new ComfyTaskResponse();
            taskRes.setPromptId(promptId);

            if (history.has(promptId)) {
                // 任务已完成
                JsonNode output = history.get(promptId).get("outputs").get("9").get("images").get(0);
                String filename = output.get("filename").asText();
                taskRes.setStatus("COMPLETED");
                taskRes.setImageUrl(COMFY_URL + "/view?filename=" + filename);
            } else {
                // 任务还在排队或执行中
                // 进阶：可以进一步调用 /queue 接口查看具体位置，这里简单处理为 RUNNING
                taskRes.setStatus("RUNNING");
            }
            return taskRes;
        }
    }
}
