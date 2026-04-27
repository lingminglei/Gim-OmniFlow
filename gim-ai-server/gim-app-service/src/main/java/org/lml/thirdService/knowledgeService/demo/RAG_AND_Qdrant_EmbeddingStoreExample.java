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
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.lml.AiService.service.AisServices;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

public class RAG_AND_Qdrant_EmbeddingStoreExample {

    private static final String baseUrl = "https://sg.uiuiapi.com/v1";
    private static final String apiKey = "sk-GKWtmZ6Uvl57o4kUoQ2Td9w73MJvnTPF5FBgmxYYA43BxH0u";
    private static final String qdrantHost = "localhost"; // Qdrant 地址
    private static final int qdrantGrpcPort = 6334;       // Qdrant gRPC端口
    private static final String qdrantCollectionName = "langchain4j_docs";

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 1. 加载文档
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("D:/file");
        System.out.println("已加载文件数量: " + documents.size());
        if (documents.isEmpty()) {
            System.err.println("警告: 未加载到任何文档！");
            return;
        }

        // 2. 初始化 QdrantClient（用于创建集合等管理操作）
        QdrantClient qdrantClient = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantGrpcPort, false).build());

        // 3. 创建 Qdrant 向量集合（若已存在可跳过）
        int dimension = 1536; // 这里一般与OpenAI Embedding向量维度一致，text-embedding-3-small一般是1536
//        qdrantClient.createCollectionAsync(
//                        qdrantCollectionName,
//                        Collections.VectorParams.newBuilder()
//                                .setDistance(Collections.Distance.Cosine)
//                                .setSize(dimension)
//                                .build())
//                .get();

        // 4. 初始化 QdrantEmbeddingStore 用于存储和搜索向量
        EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantGrpcPort)
                .collectionName(qdrantCollectionName)
                .build();

        // 5. 初始化 OpenAI Embedding Model
        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-3-small")
                .baseUrl(baseUrl)
                .build();

        // 6. 用 EmbeddingStoreIngestor 将文档向量化并写入 Qdrant
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        try {
            ingestor.ingest(documents);
            System.out.println("文档向量摄入完成");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 7. 创建基于Qdrant向量库的内容检索器
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5)
                .build();

        // 8. 构建聊天模型
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(GPT_4_O_MINI)
                .baseUrl(baseUrl)
                .build();

        // 9. 创建对话记忆管理，限制上下文
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 10. 构建 AiServices（你的自定义接口实现类名这里用示例 AisServices）
        AisServices assistant = AiServices.builder(AisServices.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();

        // 11. 测试一次问答
        String question = "我需要你帮我总结一下，凌明磊是一个什么样的人";
        String answer = assistant.chat22(question);
        System.out.println("问: " + question);
        System.out.println("答: " + answer);
    }

}

