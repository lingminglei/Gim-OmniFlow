package org.lml.thirdService.knowledgeService.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.lml.AiService.service.AisServices;
import org.lml.thirdService.knowledgeService.utils.MultiContentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    @Value("${uiuiapi.apiKey}")
    private String apiKey;

    @Value("${uiuiapi.baseUrl}")
    private String baseUrl;

    /**
     * 集成 Agent 增强对话服务
     *
     * @param question 用户提问的问题
     * @param userId 用户ID，用于区分不同用户的知识库
     * @return 基于知识库增强的对话回答
     */
    public String askWithKnowledgeBase(String question, List<String> kbCodes, String userId) {

        // 配置OpenAI嵌入模型，用于将文本转换为向量表示
        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("text-embedding-3-small")
                .build();

        List<ContentRetriever> retrievers = kbCodes.stream()
                .map(kb -> {
                    String collectionName = kb + "_" + userId;
                    EmbeddingStore<TextSegment> store = QdrantEmbeddingStore.builder()
                            .host("localhost")
                            .port(6334)
                            .collectionName(collectionName)
                            .build();
                    return (ContentRetriever) EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(store)
                            .embeddingModel(embeddingModel)
                            .maxResults(5)
                            .minScore(0.5)
                            .build();
                })
                .toList();

        ContentRetriever multiRetriever = new MultiContentRetriever(retrievers);

        // 配置OpenAI对话模型，用于生成对话回复
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .baseUrl(baseUrl)
                .build();

        // 创建对话记忆，保存最近10条对话历史
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 构建AI服务，整合对话模型、记忆和内容检索功能
        AisServices assistant = AiServices.builder(AisServices.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .contentRetriever(multiRetriever)
                .build();

        return assistant.chat22(question);
    }

}
