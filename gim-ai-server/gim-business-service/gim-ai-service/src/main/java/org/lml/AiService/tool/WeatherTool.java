package org.lml.AiService.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.lml.AiService.factory.AiModelFactoryService;
import org.lml.AiService.service.AisServices;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

@Component
@Slf4j
public class WeatherTool {

    @Resource
    private AiModelFactoryService aiModelFactoryService;

    @Tool("获取指定城市今天的天气信息，参数 city 是城市名称，例如：'北京', '上海', '南阳'")
    public String getTodayWeather(String city) {
        log.info("获取城市：{}，今天的天气信息工具调用！",city);
        return callWeatherApi(city);
    }

    private String callWeatherApi(String city) {

        return null;
    }

    /**
     * 集成 RAG 系统
     * 获取城市 location_id 信息
     */
    public String getLocationId(String city) {
        String projectRootPath = new File("").getAbsolutePath();
        String filePath = projectRootPath + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static";

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(filePath);
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        System.out.println("embeddingStore=="+embeddingStore);

        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        System.out.println("embeddingStore=="+embeddingStore);

        AisServices assistant = AiServices.builder(AisServices.class)
                .chatModel(aiModelFactoryService.processWithOpenAi())
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();

        String message = assistant.chat1(
                "请从数据中精确查询 `location_name_zh` 字段值为 '" + city + "' 的城市记录，并返回location_id 值,格式为：location_id=123 如果该城市不存在的话，就返回null "
        );
        return message;
    }

}
