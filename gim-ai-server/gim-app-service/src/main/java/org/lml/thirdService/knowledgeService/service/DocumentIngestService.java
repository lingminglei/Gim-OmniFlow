package org.lml.thirdService.knowledgeService.service;

import cn.dev33.satoken.stp.StpUtil;
import com.google.common.util.concurrent.ListenableFuture;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class DocumentIngestService {

    @Value("${uiuiapi.apiKey}")
    private String apiKey;

    @Value("${uiuiapi.baseUrl}")
    private String baseUrl;

    @Value("${qdrant.url}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private Integer qdrantGrpcPort;

    @Value("${qdrant.file-path}")
    private String fileStorePath;

    @Value("${qdrant.embedding-model-name}")
    private String embeddingModelName;

    @Resource
    private QdrantClient qdrantClient;

    /**
     * 上传文件并加载到知识库（向量化）
     */
    public boolean ingestFromFile(MultipartFile file, String knowledgeCode) {
        String collectionName = knowledgeCode + "_" + String.valueOf(StpUtil.getLoginId());
        log.info("📥【文档向量化启动】用户: [{}], 知识库编码: [{}], 集合名称: [{}]", String.valueOf(StpUtil.getLoginId()), knowledgeCode, collectionName);

        // === Step 1: 准备存储目录 ===
        String targetDirPath = fileStorePath + File.separator + collectionName;
        File targetDir = new File(targetDirPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            log.error("❌【目录创建失败】路径: [{}]", targetDirPath);
            throw new RuntimeException("创建存储目录失败");
        }
        log.debug("✅【目录检查通过】目标目录: [{}]", targetDirPath);

        // === Step 2: 保存上传文件 ===
        File targetFile = new File(targetDir, file.getOriginalFilename());
        try {
            file.transferTo(targetFile);
            log.info("📄【文件保存成功】文件名: [{}], 存储路径: [{}]", file.getOriginalFilename(), targetFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("❌【文件保存失败】文件名: [{}], 错误信息: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("文件保存失败", e);
        }

        // === Step 3: 加载文档内容 ===
        List<Document> documents;
        try {
            documents = FileSystemDocumentLoader.loadDocuments(targetDirPath);
            if (documents.isEmpty()) {
                log.warn("⚠️【文档解析为空】文件路径: [{}]", targetFile.getAbsolutePath());
                throw new RuntimeException("未能从文件中加载有效内容");
            }
            log.info("📚【文档加载完成】解析文档数: [{}]", documents.size());
        } catch (Exception e) {
            log.error("❌【加载文档失败】路径: [{}], 错误信息: {}", targetDirPath, e.getMessage(), e);
            throw new RuntimeException("加载文档失败", e);
        }

        // === Step 4: 初始化嵌入模型与向量存储 ===
        log.debug("⚙️【初始化模型与向量存储】Qdrant主机: [{}:{}], 模型: [{}]", qdrantHost, qdrantGrpcPort, embeddingModelName);
        EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantGrpcPort)
                .collectionName(collectionName)
                .build();

        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .build();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // === Step 5: 执行文档向量化写入 ===
        try {
            log.info("🚀【开始向量化】集合: [{}], 文档数: [{}]", collectionName, documents.size());
            ingestor.ingest(documents);
            log.info("✅【向量化完成】集合: [{}], 成功写入文档数: [{}]", collectionName, documents.size());
        } catch (Exception e) {
            log.error("❌【向量化失败】集合: [{}], 错误信息: {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("文档向量化失败", e);
        }

        log.info("✅【文档向量化流程结束】集合: [{}]", collectionName);

        return true;
    }

    /**
     * 初始化向量集合（知识库维度 + 用户维度）
     *
     * @param knowledgeCode 知识库编码
     */
    public boolean initVectorCollection(String knowledgeCode) {

        String collectionName = knowledgeCode + "_" + String.valueOf(StpUtil.getLoginId());

        log.info("准备初始化向量集合: {}", collectionName);

        try {
            // 查询集合是否已存在（异步调用）
            ListenableFuture<Boolean> future = qdrantClient.collectionExistsAsync(collectionName);
            boolean collectionExists = future.get(); // 会阻塞直到结果返回

            if (collectionExists) {
                log.info("集合已存在: {}，跳过创建步骤", collectionName);
            } else {
                log.info("集合不存在，开始创建集合: {}", collectionName);

                qdrantClient.createCollectionAsync(
                        collectionName,
                        Collections.VectorParams.newBuilder()
                                .setSize(1536) // 向量维度，视你的模型决定
                                .setDistance(Collections.Distance.Cosine)
                                .build()
                ).get();

                log.info("集合创建成功: {}", collectionName);

                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("初始化集合时被中断: {}", e.getMessage(), e);
            return false;
        } catch (ExecutionException e) {
            log.error("检查或创建集合时出错: {}", e.getCause().getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * 根据知识库编码查询 Qdrant 向量数据，并集成到模型上下文中
     */
    public String queryFromQdrantAndIntegrate(String knowledgeCode, String question) {
        String collectionName = knowledgeCode + "_111";

        log.info("📥【开始查询 Qdrant 向量数据】集合: [{}]", collectionName);

        // Step 1: 初始化 Qdrant 向量检索器
        EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantGrpcPort)
                .collectionName(collectionName)
                .build();

        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .build();

        // 使用 `EmbeddingStoreContentRetriever` 从 Qdrant 检索相关文档
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)  // 设置返回的最大结果数
                .minScore(0.5)  // 设置最小匹配得分
                .build();

        Query query = new Query(question);
        // Step 2: 查询与用户问题相关的文档
        List<Content> relevantDocuments = contentRetriever.retrieve(new Query(question));
        if (relevantDocuments.isEmpty()) {
            log.warn("⚠️【没有找到相关文档】查询问题: [{}]", question);
            return "没有找到相关信息。";
        }

        // Step 3: 构建增强的上下文
        StringBuilder enhancedContext = new StringBuilder();
        for (Content document : relevantDocuments) {
            enhancedContext.append(document.textSegment().text()).append("\n");
        }

        return enhancedContext.toString();
    }

}
