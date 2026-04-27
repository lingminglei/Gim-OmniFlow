package org.lml.AiService.factory;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 接入模型
 */
@Service
public class AiModelFactoryService {

    @Autowired
    private OpenAiModelFactory modelFactory;

    /**
     * 使用 Kimi 模型处理请求
     */
    public OpenAiChatModel processWithKimi() {
        // 动态获取 Kimi 模型的 ChatLanguageModel 实例
        OpenAiChatModel kimiModel = modelFactory.getModel("kimi");

        return kimiModel;
    }

    public OpenAiStreamingChatModel processWithKimiStream() {
        // 动态获取 Kimi 模型的 ChatLanguageModel 实例
        OpenAiStreamingChatModel kimiModel = modelFactory.getStreamingModel("kimi");

        return kimiModel;
    }

    public OpenAiStreamingChatModel processWithOllamaStream() {
        // deepseek-r1-tool-calling:8b
        OpenAiStreamingChatModel kimiModel = modelFactory.getStreamingModel("localDeepseek");

        return kimiModel;
    }

    public OpenAiStreamingChatModel processWithOllamaGptOssStream() {
        // gpt-oss:20b
        OpenAiStreamingChatModel kimiModel = modelFactory.getStreamingModel("localGptOss");

        return kimiModel;
    }



    /**
     * 处理 OpenAI 模型请求处理
     */
    public OpenAiChatModel processWithOpenAi() {
        // 动态获取默认模型的实例
        OpenAiChatModel defaultModel = modelFactory.getModel("uiuiapi");

        return defaultModel;
    }


    /**
     * 处理 DeepSeek [deepseek-chat] 模型请求处理
     *
     * 专注于对话和自然语言交流的模型，适合于聊天机器人、虚拟助手等场景
     */
    public OpenAiChatModel processWithDeepSeekChat() {
        // 动态获取默认模型的实例
        OpenAiChatModel defaultModel = modelFactory.getModel("deepSeekChat");

        return defaultModel;
    }

    /**
     * 处理 DeepSeek [deepseek-reasoner] 模型请求处理
     *
     * 侧重于推理、逻辑分析和复杂问题求解，适合用于知识推理、决策支持、数据分析等领域。
     */
    public OpenAiChatModel processWithDeepSeekReasoner() {
        // 动态获取默认模型的实例
        OpenAiChatModel defaultModel = modelFactory.getModel("deepSeekReasoner");

        return defaultModel;
    }
}
