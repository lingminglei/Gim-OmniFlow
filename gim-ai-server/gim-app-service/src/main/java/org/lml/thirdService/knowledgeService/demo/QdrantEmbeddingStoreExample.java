package org.lml.thirdService.knowledgeService.demo;

import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.UUID.randomUUID;

public class QdrantEmbeddingStoreExample {

    private static int grpcPort = 6334;  // 本地 Qdrant gRPC 端口
    private static String collectionName = "langchain4j-" + randomUUID();
    private static Collections.Distance distance = Collections.Distance.Cosine;
    private static int dimension = 384; // AllMiniLmL6V2EmbeddingModel 输出向量维度

    /**
     * 主函数，演示了如何使用 Qdrant 向量数据库进行文本嵌入存储与相似性检索。
     * <p>
     * 主要流程包括：
     * 1. 初始化 QdrantEmbeddingStore 和 QdrantClient；
     * 2. 创建向量集合；
     * 3. 使用嵌入模型对文本进行编码并存入数据库；
     * 4. 对查询文本进行编码并执行相似性搜索；
     * 5. 输出最相似的文本及其得分。
     *
     * @param args 命令行参数（未使用）
     * @throws ExecutionException   异步执行异常
     * @throws InterruptedException 线程中断异常
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        String host = "localhost";

        // 创建 QdrantEmbeddingStore 实例，用于向量的增删查操作
        EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                .host(host)
                .port(grpcPort)
                .collectionName(collectionName)
                .build();

        // 创建 QdrantClient 实例，用于管理集合等操作
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, grpcPort, false).build());

        // 创建集合，必须先创建集合才能进行向量插入和查询操作
        client.createCollectionAsync(
                        collectionName,
                        Collections.VectorParams.newBuilder()
                                .setDistance(distance)
                                .setSize(dimension)
                                .build())
                .get();

        // 初始化嵌入模型，用于将文本转换为向量表示
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // 插入第一个文本及其嵌入向量到数据库中
        TextSegment segment1 = TextSegment.from("张三高考720分");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        // 插入第二个文本及其嵌入向量到数据库中
        TextSegment segment2 = TextSegment.from("张三很喜欢吃苹果");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        // 对查询语句进行嵌入编码，准备进行相似性搜索
        Embedding queryEmbedding = embeddingModel.embed("张三是一个什么样的人").content();

        // 构造搜索请求，指定查询向量和最大返回结果数
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();

        // 执行向量相似性搜索，并获取匹配结果
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
        if (!matches.isEmpty()) {
            EmbeddingMatch<TextSegment> embeddingMatch = matches.get(0);
            System.out.println("相似度得分: " + embeddingMatch.score());
            System.out.println("匹配文本: " + embeddingMatch.embedded().text());
        } else {
            System.out.println("没有找到匹配项");
        }
    }

}
