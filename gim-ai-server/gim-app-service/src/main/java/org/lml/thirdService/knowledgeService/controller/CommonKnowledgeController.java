package org.lml.thirdService.knowledgeService.controller;

import org.lml.thirdService.knowledgeService.service.DocumentIngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@RequestMapping("/fileKnowledge")
public class CommonKnowledgeController {

    @Resource
    private DocumentIngestService documentIngestService;

    /**
     * 上传文件到知识库
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("knowledgeCode") String knowledgeCode) {
        // 保存本地 + 解析文档 + 写入向量库
        documentIngestService.ingestFromFile(file, knowledgeCode);
        return ResponseEntity.ok("上传并写入成功");
    }

    /**
     * 创建知识库
     */
    @GetMapping("/create")
    public ResponseEntity<?> createKnowledge(@RequestParam("knowledgeCode") String knowledgeCode){
        documentIngestService.initVectorCollection(knowledgeCode);

        return ResponseEntity.ok("创建成功");
    }
}
