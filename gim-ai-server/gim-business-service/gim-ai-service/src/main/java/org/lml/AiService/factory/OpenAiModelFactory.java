package org.lml.AiService.factory;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.lml.AiService.config.AiModelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static java.time.Duration.ofSeconds;

/**
 * OpenAiModel工厂类。
 * 负责根据配置属性（AiModelProperties）动态构建和缓存不同模型的 OpenAiChatModel 实例。
 * 实现了模型配置的集中管理和动态切换。
 */
@Service
@Slf4j
public class OpenAiModelFactory {

    /** 存储从配置文件中加载的模型配置属性 */
    private final AiModelProperties properties;

    /** * 模型实例缓存，用于实现单例模式和性能优化。
     * key: 模型别名 (如 "kimi", "uiuiapi")
     * value: 对应的 OpenAiChatModel 实例
     */
    private final Map<String, OpenAiChatModel> modelCache = new ConcurrentHashMap<>();

    /** 流式模型缓存 (新增) */
    private final Map<String, OpenAiStreamingChatModel> streamingModelCache = new ConcurrentHashMap<>();

    /**
     * 构造函数：注入配置属性 Bean
     * @param properties 自动注入的 AiModelProperties 配置实例
     */
    @Autowired
    public OpenAiModelFactory(AiModelProperties properties) {
        this.properties = properties;
    }

    /**
     * 核心方法：根据模型别名动态获取 OpenAiChatModel 实例
     * 优先从缓存中获取，如果不存在则根据配置构建新实例并加入缓存（懒加载）。
     * * @param modelAlias 模型配置的别名 (如 "kimi", "default")
     * @return 对应的 OpenAiChatModel 实例
     * @throws IllegalArgumentException 如果配置文件中未找到对应的模型配置
     */
    public OpenAiChatModel getModel(String modelAlias) {
        // 1. 尝试从缓存中获取已创建的模型实例
        if (modelCache.containsKey(modelAlias)) {
            log.info("从缓存中获取模型实例: {}", modelAlias);
            return modelCache.get(modelAlias);
        }

        // 打印当前所有已加载的配置，用于调试确认配置是否成功读取
        log.info("当前已加载的模型配置Keys: {}", properties.getConfigs().keySet());

        // 2. 获取配置
        AiModelProperties.ModelConfig config = properties.getConfigs().get(modelAlias);
        if (config == null) {
            log.error("未找到模型别名: {} 的配置信息，请检查 application.yml", modelAlias);
            // 如果配置不存在，抛出异常，阻止创建失败的模型
            throw new IllegalArgumentException("未找到 AI 模型配置: " + modelAlias);
        }

        // 3. 构建模型实例
        log.info("开始构建新的 OpenAiChatModel 实例：{}", modelAlias);
        OpenAiChatModel model = buildModel(config);

        // 4. 存入缓存并返回
        modelCache.put(modelAlias, model);
        log.info("动态创建并缓存了 OpenAiChatModel 实例：{}", modelAlias);

        return model;
    }

    /** * 获取流式模型
     */
    public OpenAiStreamingChatModel getStreamingModel(String modelAlias) {
        return streamingModelCache.computeIfAbsent(modelAlias, alias -> {
            log.info("构建流式模型实例: {}", alias);
            return buildStreamingModel(getConfig(alias));
        });
    }

    /**
     * 使用 ModelConfig 中的参数构建 LangChain4j 的 OpenAiChatModel 实例。
     * @param config 单个模型的配置参数对象
     * @return 配置完成的 OpenAiChatModel
     */
    private OpenAiChatModel buildModel(AiModelProperties.ModelConfig config) {
        // 使用 LangChain4j 提供的 Builder 模式配置模型参数
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey()) // API 认证密钥，必须提供
                .modelName(config.getModelName()) // AI 模型的名称 (如 gpt-3.5-turbo, kimi-k2-0905-preview)
                .temperature(config.getTemperature()) // 模型的创造性/随机性 (0.0 表示确定性，1.0+ 表示高随机性)
                .timeout(ofSeconds(config.getTimeoutSeconds())) // 设置 API 请求的超时时间 (转换为 Duration 类型)
                .maxTokens(config.getMaxTokens()) // 限制模型响应的最大 Token 数量，控制成本和响应长度
                .frequencyPenalty(config.getFrequencyPenalty()) // 频率惩罚：减少模型重复相同 Token 的概率
                .logRequests(config.isLogRequests()) // 启用日志，记录发送给 API 的请求体（调试用）
                .logResponses(config.isLogResponses()) // 启用日志，记录从 API 接收到的响应体（调试用）
                .baseUrl(config.getBaseUrl()) // 目标 API 地址 (如 OpenAI 官方地址或 Kimi/UIUIAPI 的兼容地址)
                // 启用 JSON 格式输出的能力。对于需要结构化数据返回的场景（如 function calling, Pydantic model），必须设置
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                // 严格遵守 JSON Schema：要求模型返回的 JSON 必须与指定的 Schema 严格匹配。
                // 如果目标 API 不支持或模型在 JSON 模式下表现不稳定，建议移除或设置为 false。
                .strictJsonSchema(true)
                .build();
    }

    /** * 构建流式模型 (新增)
     */
    private OpenAiStreamingChatModel buildStreamingModel(AiModelProperties.ModelConfig config) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl())
                .temperature(config.getTemperature())
                .timeout(ofSeconds(config.getTimeoutSeconds()))
                // 注意：流式模型通常不需要 strictJsonSchema，除非有特殊结构化需求
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses())
                .build();
    }

    private AiModelProperties.ModelConfig getConfig(String modelAlias) {
        AiModelProperties.ModelConfig config = properties.getConfigs().get(modelAlias);
        if (config == null) {
            throw new IllegalArgumentException("未找到 AI 模型配置: " + modelAlias);
        }
        return config;
    }
}
