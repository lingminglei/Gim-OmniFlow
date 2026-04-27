package org.lml.thirdService.knowledgeService.demo;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.lml.AiService.service.AisServices;


import java.util.List;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;


public class Naive_RAG_Example {

    private static final String baseUrl = "https://sg.uiuiapi.com/v1";

    private static final String apiKey = "sk-GKWtmZ6Uvl57o4kUoQ2Td9w73MJvnTPF5FBgmxYYA43BxH0u";

    /**
     * 程序主入口，用于加载文档、构建嵌入模型与检索系统，并基于AI服务进行问答交互。
     */
    public static void main(String[] args) {

        // 加载指定目录下的所有文档
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("D:/file");

        System.out.println("已加载文件数量: " + documents.size());
        if (documents.isEmpty()) {
            System.err.println("警告: 未从文件夹中加载到任何文档！");
        } else {
            for (Document doc : documents) {
                System.out.println("文档内容预览: " + doc.toTextSegment());
            }
        }

        // 创建内存中的嵌入存储，用于保存文档的向量表示
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 构建OpenAI嵌入模型实例，用于将文本转换为向量
        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey) // 替换为你的 API 密钥
                .modelName("text-embedding-3-small") // 使用合适的嵌入模型名称
                .baseUrl(baseUrl)
                .build();

        /**
         * 构建嵌入存储摄入器，负责将文档通过嵌入模型处理后存入嵌入存储中
         */
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        /**
         * 构建内容检索器，用于根据查询从嵌入存储中检索最相关的文本片段
         */
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();


        // 执行文档摄入操作，将文档向量化并存入嵌入存储
        try {
            ingestor.ingest(documents);
            System.out.println("Ingest completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 构建聊天语言模型，用于生成自然语言回复
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey) // 替换为你的 API 密钥
                .modelName(GPT_4_O_MINI)
                .baseUrl(baseUrl)
                .build();

        // 创建对话记忆管理器，限制最大消息数以控制上下文长度
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 构建AI助手服务，整合聊天模型、内容检索器和对话记忆
        AisServices assistant = AiServices.builder(AisServices.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();

        // 启动一次问答交互示例
        System.out.println(assistant.chat22("凌明磊有什么特长和技术"));

    }


}
