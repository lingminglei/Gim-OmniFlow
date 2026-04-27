package org.lml.thirdService.comfyUi;

import org.lml.AiGateWay.config.WorkflowResourceReader;
import org.lml.thirdService.comfyUi.dto.ComfyTaskResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comfy")
public class ComfyController {

    @Autowired
    private ComfyUIService comfyUIService;
    @Autowired
    private WorkflowResourceReader reader;

    /**
     * 接口 1: 异步发起绘画
     * 返回 promptId
     */
    @PostMapping("/draw")
    public ResponseEntity<String> draw(@RequestParam String prompt) {
        try {
            String baseJson = reader.readWorkflow("textToImage.json");
            long seed = (long) (Math.random() * Long.MAX_VALUE);

            String promptId = comfyUIService.submitTask(baseJson, prompt, seed);
            return ResponseEntity.ok(promptId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("提交失败: " + e.getMessage());
        }
    }

    /**
     * 接口 2: 根据 ID 查询进度/结果
     */
    @GetMapping("/status/{promptId}")
    public ResponseEntity<ComfyTaskResponse> getStatus(@PathVariable String promptId) {
        try {
            ComfyTaskResponse response = comfyUIService.checkTaskStatus(promptId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
