package org.lml.thirdService.minIO.service;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

@Slf4j
@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String publicHost;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO 存储桶 [{}] 初始化成功", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化 MinIO 失败: ", e);
        }
    }

    /**
     * 1. 适配 MultipartFile (用于 Controller 接口)
     */
    public String uploadImage(MultipartFile file) {
        try {
            return uploadInputStream(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new RuntimeException("MultipartFile 上传失败", e);
        }
    }

    /**
     * 2. 适配本地文件 (用于服务器本地磁盘文件转存)
     */
    public String uploadLocalFile(File file) {
        if (!file.exists()) {
            throw new RuntimeException("本地文件不存在: " + file.getAbsolutePath());
        }
        try (InputStream is = new FileInputStream(file)) {
            // 注意：本地 File 无法自动获取 Content-Type，这里可以手动指定或使用推断
            return uploadInputStream(is, file.getName(), "image/jpeg", file.length());
        } catch (Exception e) {
            throw new RuntimeException("本地文件上传失败", e);
        }
    }

    /**
     * 3. 适配网络 URL (用于 AI 结果 URL 转存)
     * @param fileUrl 远程图片下载地址
     */
    public String uploadUrlFile(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000); // 生产环境必须设置超时
            conn.setReadTimeout(10000);

            try (InputStream is = conn.getInputStream()) {
                // 尝试从 URL 中截取文件名
                String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf("?"));

                return uploadInputStream(is, fileName, conn.getContentType(), conn.getContentLengthLong());
            }
        } catch (Exception e) {
            log.error("URL 转存失败: {}", fileUrl, e);
            throw new RuntimeException("网络资源转存失败");
        }
    }

    /**
     * 核心私有上传逻辑 (收口方法)
     */
    private String uploadInputStream(InputStream is, String originalFilename, String contentType, long size) {
        String suffix = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "") + suffix;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                            .stream(is, size, -1) // size 为 -1 时表示未知长度，但已知长度时性能更好
                            .build()
            );
            return String.format("%s/%s/%s", publicHost, bucketName, objectName);
        } catch (Exception e) {
            log.error("MinIO 写入异常: ", e);
            throw new RuntimeException("文件写入 OSS 失败");
        }
    }
}
