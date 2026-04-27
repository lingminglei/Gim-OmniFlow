package org.lml.thirdService.minIO.controller;

import org.lml.thirdService.minIO.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/minio/file")
public class MinIoFileController {

    @Autowired
    private MinioService minioService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {

            String url = minioService.uploadImage(file);

            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("上传失败：" + e.getMessage());
        }
    }
}
