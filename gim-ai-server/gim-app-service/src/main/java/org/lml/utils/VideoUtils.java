package org.lml.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import org.lml.entity.dto.ai.SegmentInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class VideoUtils {

    // 获取存储路径
    @Value("${ufop.local-storage-path}")
    public String filePath;


    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> videoPaths = Arrays.asList(
                "D:\\file\\aiVideo\\2025-11-28\\6084a72e-b3d4-4243-95c2-50de0c11e509.mp4",
                "D:\\file\\aiVideo\\2025-11-28\\53ccec84-a398-4695-872b-0deaa95ed4e0.mp4",
                "D:\\file\\aiVideo\\2025-11-28\\_877629ddc84c25c7d79a011cdbc84827_67cfb113-0aa6-4657-b5dd-06827544a0a4.mp4"
        );
        mergeVideos(videoPaths,"D:\\fileStoragepPath\\upload\\20251128\\merge.mp4");
    }

    /**
     * 合并多个视频文件，保持顺序
     *
     * @param videoPaths 视频文件路径列表
     * @param output 合并后的视频输出路径
     * @return 合并后的视频路径
     */
    public static String mergeVideos(List<String> videoPaths, String output) throws IOException, InterruptedException {
        // 创建一个临时文件列表，用来存放需要合并的视频文件
        String fileListPath = "fileList.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileListPath))) {
            // 将视频文件路径按顺序写入文件列表
            for (String videoPath : videoPaths) {
                writer.write("file '" + videoPath + "'\n");
            }
        }

        // 构建 FFmpeg 命令，使用 concat 模式按顺序合并视频
        String command = String.format(
                "ffmpeg -f concat -safe 0 -i %s -c copy %s",
                fileListPath,  // 临时文件列表
                output         // 输出文件路径
        );

        // 执行 FFmpeg 命令
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        // 删除临时文件列表
        new File(fileListPath).delete();

        if (exitCode == 0) {
            System.out.println("视频合并成功，输出文件: " + output);
        } else {
            System.err.println("视频合并失败");
        }

        return output;
    }

    /**
     * 提取视频的尾帧并保存为图片
     * @param inputVideoPath 输入视频路径
     * @return 返回保存的图片路径
     * @throws IOException
     */
    public String extractLastFrame(String inputVideoPath) throws IOException {
        // 生成唯一的文件名（例如：UUID）
        String fileName = UUID.randomUUID().toString() + ".jpg";

        // 创建目标文件的路径
        String dateDir = DateUtil.format(new Date(), "yyyyMMdd");  // 创建日期文件夹（可选）
        String finalDirPath = filePath + File.separator +"upload"+File.separator+dateDir;

        // 创建文件夹（如果不存在）
        File dir = new File(finalDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成完整的文件路径
        String outputImagePath = finalDirPath + File.separator + fileName;

        // FFmpeg命令行，提取视频的最后一帧
        String command = String.format(
                "ffmpeg -sseof -1 -i %s -update 1 -q:v 1 %s",  // 使用 -sseof -1 获取视频的最后一帧
                inputVideoPath,  // 输入视频路径
                outputImagePath   // 输出图片路径
        );

        // 执行 FFmpeg 命令
        executeFFmpegCommand(command);

        // 返回保存的图片路径
        return outputImagePath;
    }

    /**
     * 执行 FFmpeg 命令并处理结果
     * @param command FFmpeg 命令
     * @throws IOException
     */
    private void executeFFmpegCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try {
            // 等待命令执行完毕
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("成功执行 FFmpeg 命令：" + command);
            } else {
                System.err.println("执行 FFmpeg 命令失败：" + command);
            }
        } catch (InterruptedException e) {
            System.err.println("FFmpeg 命令执行被中断：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 多分片提示词信息生成单个提示词
     * @param segmentInfoList
     * @return
     */
    public static String getVideoPromtString(List<SegmentInfo> segmentInfoList){
        StringBuffer stringMessage = new StringBuffer();


        stringMessage.append("【AI视频分镜脚本提示词】\n");
        stringMessage.append("---------------------------------------\n");

        int counter = 1;
        for (SegmentInfo segment : segmentInfoList) {
            // 2. 提示词生成: 结构化地拼接每个分片的信息
            stringMessage.append("--- 分片 ").append(counter++).append(" (").append(segment.getBeginTime()).append("s-").append(segment.getEndTime()).append("s) ---\n");

            // 关键描述部分
            stringMessage.append("标题: ").append(segment.getSegmentTitle()).append("\n");
            stringMessage.append("场景描述: ").append(segment.getSegmentDescription()).append("\n");
            stringMessage.append("关键词/风格: ").append(segment.getSegmentKeywords()).append(" | ").append(segment.getShardingStyle()).append("\n");

            // 核心技术指令（作为提示词的核心）
            stringMessage.append("▶️ 构图: ").append(segment.getFragmentedComposition()).append("\n");
            stringMessage.append("💡 光影: ").append(segment.getFragmentedLightAndShadow()).append("\n");
            stringMessage.append("🎥 运镜: ").append(segment.getFragmentedCameraMovement()).append("\n\n");
        }

        stringMessage.append("---------------------------------------\n");
        stringMessage.append("请根据以上技术要求和描述，生成最终视频。");

        return stringMessage.toString();
    }

    /**
     * 单分片提示词信息生成单个提示词
     */
    public static String getSingleSegmentPromptString(SegmentInfo segment) {
        StringBuffer stringMessage = new StringBuffer();

        stringMessage.append("【AI视频分镜脚本提示词】\n");
        stringMessage.append("---------------------------------------\n");

        // 生成单个分片的提示词
        stringMessage.append("--- 分片 (").append(segment.getBeginTime()).append("s-").append(segment.getEndTime()).append("s) ---\n");

        // 关键描述部分
        stringMessage.append("标题: ").append(segment.getSegmentTitle()).append("\n");
        stringMessage.append("场景描述: ").append(segment.getSegmentDescription()).append("\n");
        stringMessage.append("关键词/风格: ").append(segment.getSegmentKeywords()).append(" | ").append(segment.getShardingStyle()).append("\n");

        // 核心技术指令（作为提示词的核心）
        stringMessage.append("▶️ 构图: ").append(segment.getFragmentedComposition()).append("\n");
        stringMessage.append("💡 光影: ").append(segment.getFragmentedLightAndShadow()).append("\n");
        stringMessage.append("🎥 运镜: ").append(segment.getFragmentedCameraMovement()).append("\n\n");

        // 额外的注意事项
        stringMessage.append("💬 任务一致性：确保本分片与前后分片的情节和视觉风格保持一致，避免出现突兀的画面变化。\n");

        // 强调图片参考和任务一致性
        stringMessage.append("🖼️ 图片参考：根据上传的图片描述，确保本分片的视觉风格、人物、构图和光影等与图片保持一致，体现出任务一致性。\n");
        stringMessage.append("   请注意：确保图像中的元素（如人物服饰、光影、构图等）在分片中得以忠实呈现，保持画面的一致性和连贯性。\n");

        // 镜头切换连贯性
        stringMessage.append("🎬 镜头切换流畅性：特别注意分片首帧的镜头切换与前一分片的流畅过渡，保持视觉上的连续性。\n");
        stringMessage.append("🎥 运镜注意：与前后分片镜头保持一致，避免出现突兀的过渡，确保镜头平滑衔接。\n");
        stringMessage.append("📌 连接前后分片：确保与前后分片的镜头、动作、氛围等有良好的衔接，避免出现突兀的过渡。\n");

        stringMessage.append("---------------------------------------\n");
        stringMessage.append("请根据以上技术要求和描述，生成该分片的视频，并确保其在电影中的整体一致性与流畅性。");

        return stringMessage.toString();
    }
}
