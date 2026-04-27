package org.lml.thirdService.knowledgeService.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  QdrantClient 初始加载
 */
@Slf4j
@Configuration
public class QdrantConfig {

    @Value("${qdrant.url}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private int qdrantGrpcPort;

    @Bean
    public QdrantClient qdrantClient() {
        long startTime = System.currentTimeMillis();

        if (qdrantHost == null || qdrantHost.isEmpty()) {
            log.error("❌ Qdrant 初始化失败：qdrant.host 配置为空");
            throw new IllegalArgumentException("Qdrant host must not be null or empty");
        }

        if (qdrantGrpcPort <= 0 || qdrantGrpcPort > 65535) {
            log.error("❌ Qdrant 初始化失败：qdrant.grpc-port 配置非法 -> {}", qdrantGrpcPort);
            throw new IllegalArgumentException("Qdrant gRPC port is invalid: " + qdrantGrpcPort);
        }

        try {
            QdrantClient client = new QdrantClient(
                    QdrantGrpcClient.newBuilder(qdrantHost, qdrantGrpcPort, false).build()
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Qdrant 客户端连接成功，Host: {}, Port: {}, 耗时: {} ms", qdrantHost, qdrantGrpcPort, duration);

            return client;
        } catch (Exception e) {
            log.error("❌ Qdrant 客户端连接失败，Host: {}, Port: {}，错误信息: {}", qdrantHost, qdrantGrpcPort, e.getMessage(), e);
            throw new RuntimeException("Qdrant client initialization failed", e);
        }
    }
}
