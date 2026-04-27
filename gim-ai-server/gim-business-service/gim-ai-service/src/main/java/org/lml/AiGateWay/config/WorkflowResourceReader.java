package org.lml.AiGateWay.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * ComfyUI 工作流读取器。
 */
@Component
public class WorkflowResourceReader {

    private final ResourceLoader resourceLoader;

    public WorkflowResourceReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 读取指定工作流文件内容。
     */
    public String readWorkflow(String fileName) {
        try {
            Resource resource = resourceLoader.getResource("classpath:comfy_workflows/" + fileName);
            try (Scanner scanner = new Scanner(resource.getInputStream(), StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取工作流文件: " + fileName, e);
        }
    }
}
