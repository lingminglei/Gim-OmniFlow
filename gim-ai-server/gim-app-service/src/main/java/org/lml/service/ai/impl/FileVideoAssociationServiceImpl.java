package org.lml.service.ai.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.UUID;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.lml.entity.dto.ai.FileVideoAssociation;
import org.lml.entity.dto.ai.UserVideo;
import org.lml.mapper.ai.FileVideoAssociationMapper;
import org.lml.service.ai.IFileVideoAssociationService;
import org.lml.service.ai.IUserVideoService;
import org.lml.utils.HttpUtils;
import org.lml.utils.VideoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.HashMap;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-11-21
 */
@Service
@Slf4j
public class FileVideoAssociationServiceImpl extends ServiceImpl<FileVideoAssociationMapper, FileVideoAssociation> implements IFileVideoAssociationService {


    @Value("${aiVideo.path}")
    private String videoFilePath;

    @Value("${ufop.local-storage-path}")
    private String filePath;

    @Resource
    private VideoUtils videoUtils;

    @Resource
    private IUserVideoService iUserVideoService;


    /**
     * 异步下载视频
     * @param videoUrl 视频的远程 URL 地址
     */
    @Async("taskExecutor")
    @Override
    public void downloadVideo(String videoUrl) {
        System.out.println("开始下载任务: " + videoUrl);
        long startTime = System.currentTimeMillis();

        try {
            String dateDir = DateUtil.format(new Date(), "yyyy-MM-dd");
            String finalDirPath = videoFilePath + File.separator + dateDir;

            if (!FileUtil.exist(finalDirPath)) {
                FileUtil.mkdir(finalDirPath);
            }

            String fileName = UUID.randomUUID().toString() + ".mp4";
            String targetFilePath = finalDirPath + File.separator + fileName;

            UserVideo userVideo = new UserVideo();
            userVideo.setUserId(String.valueOf(StpUtil.getLoginId()));
            userVideo.setCreator(videoUrl);//视频预览地址
            userVideo.setIsActive(true);
            userVideo.setRemarks(fileName);//文件名称
            userVideo.setLastModifiedBy(targetFilePath);//视频存储地址
            iUserVideoService.save(userVideo);

            long size = HttpUtil.downloadFile(videoUrl, FileUtil.file(targetFilePath), new StreamProgress() {
                @Override public void start() { System.out.println("连接成功，开始下载..."); }
                @Override public void progress(long total, long progress) {
//                    System.out.println("已下载: " + progress + "/" + total);
                }
                @Override public void finish() {
                    System.out.println("下载完成！");

                }
            });

            long costTime = System.currentTimeMillis() - startTime;
            log.info("视频保存成功！路径：{}，大小：{}字节，耗时：{}ms", targetFilePath, size, costTime);

        } catch (Exception e) {
            log.error("视频下载失败: {}", e.getMessage(), e);
        }
    }

    @Async("taskExecutorImage")
    @Override
    public void downloadImage(String videoUrl) {
        System.out.println("开始下载任务: " + videoUrl);
        long startTime = System.currentTimeMillis();

        try {
            String dateDir = DateUtil.format(new Date(), "yyyy-MM-dd");
            String finalDirPath = videoFilePath + File.separator + dateDir;

            if (!FileUtil.exist(finalDirPath)) {
                FileUtil.mkdir(finalDirPath);
            }

            String fileName = UUID.randomUUID().toString() + ".png";
            String targetFilePath = finalDirPath + File.separator + fileName;

            UserVideo userVideo = new UserVideo();
            /**
             * TODO: 这里暂时写死，后续需要把用户ID 放在线程中，避免消费者线程，在处理消费信息的时候无法获取登录用户信息
             */
//            userVideo.setUserId(String.valueOf(StpUtil.getLoginId()));
            userVideo.setUserId("111");
            userVideo.setCreator(videoUrl);//文件预览地址
            userVideo.setIsActive(true);
            userVideo.setRemarks(fileName);//文件名称
            userVideo.setLastModifiedBy(targetFilePath);//文件存储地址
            iUserVideoService.save(userVideo);

            long size = HttpUtil.downloadFile(videoUrl, FileUtil.file(targetFilePath), new StreamProgress() {
                @Override public void start() { System.out.println("连接成功，开始下载..."); }
                @Override public void progress(long total, long progress) {
//                    System.out.println("已下载: " + progress + "/" + total);
                }
                @Override public void finish() {
                    System.out.println("下载完成！");
                }
            });

            long costTime = System.currentTimeMillis() - startTime;
            log.info("图片保存成功！路径：{}，大小：{}字节，耗时：{}ms", targetFilePath, size, costTime);

        } catch (Exception e) {
            log.error("图片下载失败: {}", e.getMessage(), e);
        }
    }

    private static final int MAX_RETRIES = 10; // 最大重试次数
    private static final long RETRY_DELAY_MS = 2000; // 每次重试的延迟时间（单位：毫秒）

    public String getPictureUrl(String filePaths) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                // 发起文件上传请求
                HttpResponse response = HttpUtils.doPostFile("http://47.120.60.61:9876",
                        "/file/upload",
                        new HashMap<>(),
                        new HashMap<>(),
                        filePaths);

                // 处理响应
                if (response != null) {
                    String json = EntityUtils.toString(response.getEntity(), "UTF-8");

                    JSONObject root = JSON.parseObject(json);
                    JSONArray list = root.getJSONArray("data");
                    String firstElement = list.getString(0); // 获取第一个元素，假设是字符串类型

                    // 打印响应信息
                    System.out.println("响应码: " + response.getStatusLine().getStatusCode());
                    log.info(response.getStatusLine().getReasonPhrase());

                    log.info("文件存储地址：{}","http://47.120.60.61:9876/"+firstElement);

                    return "http://47.120.60.61:9876/"+firstElement;
                } else {
                    System.err.println("文件上传失败");
                }

            } catch (Exception e) {
                log.error("文件存储到文件服务器失败，第 {} 次重试", attempt + 1, e);
                attempt++;

                // 如果尝试次数未达到最大重试次数，延迟一段时间后再重试
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS); // 延迟 1 秒
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();  // 恢复中断状态
                    }
                }
            }
        }

        // 如果达到最大重试次数仍然失败，返回空字符串或其他适当的错误信息
        log.error("文件上传请求失败，已尝试 {} 次", MAX_RETRIES);
        return "";
    }


}
