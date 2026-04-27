package org.lml.controller.ai;

import lombok.extern.slf4j.Slf4j;
import org.lml.AiService.config.AiModelProperties;
import org.lml.entity.resp.ModelInfoDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型配置查询接口。
 * 对外提供已加载的 AI 模型配置信息。
 */
@RestController
@RequestMapping("/ai/models")
@Slf4j
public class AiModelController {

    private final AiModelProperties modelProperties;

    // 构造器注入 AiModelProperties
    public AiModelController(AiModelProperties modelProperties) {
        this.modelProperties = modelProperties;
    }

    /**
     * 查询所有已加载的模型配置的摘要信息。
     * 为了安全，不返回敏感的 ApiKey 和 BaseUrl。
     * @return 包含模型别名和名称的 Map
     */
    @GetMapping("/list")
    public List<ModelInfoDTO> getAllModelInfo() {
        return modelProperties.getConfigs().entrySet().stream()
                .map(entry -> {
                    AiModelProperties.ModelConfig config = entry.getValue();
                    String alias = entry.getKey();

                    // 构建 DTO 对象，只包含需要的字段
                    return ModelInfoDTO.builder()
                            .alias(alias)
                            .modelName(config.getModelName())
                            // 确保 description 即使在配置文件中未设置也不会导致崩溃
                            .description(config.getDescription() != null ? config.getDescription() : "N/A")
                            .baseUrl(config.getBaseUrl())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
