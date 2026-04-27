package org.lml.thirdService.knowledgeService.config;

import org.lml.thirdService.knowledgeService.service.DocumentIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class VectorDbInitializer implements ApplicationRunner {

    @Autowired
    private DocumentIngestService documentIngestService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 示例：从本地目录加载文档，支持知识库编码+用户维度
        String knowledgeCode = "kb001";
        String userId = "user001";
//        documentIngestService.ingestFromPath("D:/file", knowledgeCode, userId);
    }
}
