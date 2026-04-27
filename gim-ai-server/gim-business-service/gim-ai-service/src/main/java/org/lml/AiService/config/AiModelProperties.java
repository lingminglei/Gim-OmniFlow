package org.lml.AiService.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 模型配置属性类。
 * 负责从配置文件（如 application.yml）中读取所有 AI 模型的详细配置，
 * 采用 Map 结构存储，实现动态模型切换的配置基础。
 */
@Data
@Component // 标记为 Spring Bean，使其能够被扫描和注入
@ConfigurationProperties(prefix = "ai.models") // 绑定配置文件中以 "ai.models" 开头的属性
@Slf4j // 启用日志功能
public class AiModelProperties {

    /**
     * 模型配置集合。
     * key: 模型别名 (如 "kimi", "uiuiapi")
     * value: 对应模型的详细配置参数 ModelConfig
     * 初始为 HashMap，确保 getConfigs() 不会返回 null，避免 NullPointerException。
     */
    private Map<String, ModelConfig> configs = new HashMap<>();

    /**
     * Spring Bean 初始化后执行的方法。
     * 用于检查配置是否成功加载，并输出排查日志。
     */
    @PostConstruct
    public void checkLoadStatus() {
        if (configs.isEmpty()) {
            // 🚨 排查日志 1：配置加载失败或配置文件中没有定义模型
            log.warn("🚨 WARNING: AiModelProperties 加载失败或配置为空。configs Map为空，请检查 application.yml 中 ai.models 前缀配置！");
        } else {
            // ✅ 排查日志 2：配置加载成功，输出加载的模型数量和名称
            log.info("✅ SUCCESS: AiModelProperties 成功加载了 {} 个模型配置: {}",
                    configs.size(), configs.keySet());

            // ----------------------------------------------------
            // 🚀 核心优化：遍历检查所有模型配置
            // ----------------------------------------------------
            for (Map.Entry<String, ModelConfig> entry : configs.entrySet()) {
                ModelConfig config = entry.getValue();
                String alias = entry.getKey();

                // 1. 检查关键参数是否为空
                if (config.getApiKey() == null || config.getBaseUrl() == null) {
                    log.error("❌ ERROR: 模型 [{}] 配置不完整！ apiKey 或 baseUrl 为 null。请检查配置文件。", alias);
                } else {
                    // 2. 打印安全日志
                    String apiKey = config.getApiKey();
                    // 只打印 key 的最后四位，保护敏感信息
                    String safeKey = apiKey.length() > 4 ?
                            "****" + apiKey.substring(apiKey.length() - 4) :
                            "****";

                    log.info("✅ 模型 [{}] 检查通过。BaseUrl: {}, ApiKey: {}", alias, config.getBaseUrl(), safeKey);
                }
            }
        }
    }

    /**
     * 内部类：单个 AI 模型的详细配置参数
     */
    @Data
    public static class ModelConfig {
        private String apiKey;
        private String baseUrl;
        private String modelName;
        private String description;
        // 以下为可选参数，提供默认值
        private Double temperature = 0.3;
        private Integer timeoutSeconds = 60;
        private Integer maxTokens = 1000;
        private Double frequencyPenalty = 1.0;
        private boolean logRequests = true;
        private boolean logResponses = true;
    }
}
