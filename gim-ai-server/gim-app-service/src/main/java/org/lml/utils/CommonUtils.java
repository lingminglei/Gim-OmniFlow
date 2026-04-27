package org.lml.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.enums.TaskState;
import org.lml.common.exection.BizException;
import org.lml.entity.dto.ai.SegmentInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import static com.aliyun.oss.internal.OSSConstants.KB;

@Component
@Slf4j
public class CommonUtils {

    private static final Random random = new Random();

    @Resource
    private RedisUtils redisUtils;

    // 获取存储路径
    @Value("${ufop.local-storage-path}")
    public String filePath;

    // 可用的字符集（字母、数字）
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 生成一个随机用户名
     */
    public static String generateRandomUsername() {
        StringBuilder username = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            // 随机选择一个字符，并将其添加到用户名中
            int index = random.nextInt(CHARACTERS.length());
            username.append(CHARACTERS.charAt(index));
        }

        return username.toString();
    }

    /**
     * 将生成的 AI 内容保存到本地文件
     * @param content 拼接完整的文本内容
     * @param fileName 文件名（建议带上时间戳或主题）
     */
    public static void saveToFile(String content, String fileName) {
        try {
            // 确保输出目录存在（可选，这里默认保存在项目根目录）
            // 如果内容为空，则不创建文件
            if (content == null || content.trim().isEmpty()) {
                System.err.println("❌ 内容为空，取消保存文件。");
                return;
            }

            // 使用 NIO 写入文件
            Files.write(
                    Paths.get(fileName),
                    content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,        // 如果不存在则创建
                    StandardOpenOption.TRUNCATE_EXISTING // 如果存在则覆盖，避免内容重叠
            );

            System.out.println("💾 文件保存成功: " + Paths.get(fileName).toAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ 保存文件时发生 IO 异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 配合lua 脚本 + 进行状态流转前置判断
     * @return
     */
    public void transitState(String key, TaskState targetState) throws BizException {
        String allowedParents = "";

        // 根据你的逻辑定义前置规则
        switch (targetState) {
            case SUBMITTED:
                allowedParents = "INIT,SUBMITTED";
                break;
            case PROCESSING:
                allowedParents = "INIT,SUBMITTED,PROCESSING";
                break;
            case SUCCESS:
                allowedParents = "PROCESSING,SUCCESS";
                break;
            case FAIL:
                allowedParents = "INIT,SUBMITTED,PROCESSING,FAIL";
                break;
            default:
                return ;
        }

        //调用原子 CAS 脚本
        try{
            boolean success = redisUtils.compareAndSetStatus(key, allowedParents, targetState.getCode());
            if (!success) {
                log.warn("状态流转失败: 任务 {} 尝试转为 {}, 但当前状态不符", key, targetState);

                log.warn("任务 {} 状态流转跳过：当前状态不符合前置条件 {}，可能已被处理", key, allowedParents);
            }
        }catch (RedisConnectionException e) {
            // 这种属于“系统级异常”，需要抛出并触发 Kafka 重试
            throw e;
        } catch (Exception e) {
            log.error("不可预知的系统错误", e);
            throw new BizException("系统故障");
        }

    }


    /**
     * 校验手机验证码是否合法
     */
    public static boolean checkMobile(String mobile) {

        if(ObjectUtil.isNotNull(mobile) && ObjectUtil.isNotEmpty(mobile)){
            return Pattern.matches("^1[3-9]\\d{9}$", mobile);
        }

        return false;
    }

    /**
     * 生成一个指定位数的随机数字字符串
     *
     * @param length 随机数字字符串的长度
     * @return 随机数字字符串
     */
    public String randomNumbers(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // 生成0-9之间的随机数字
        }
        return sb.toString();
    }

    /**
     * 计算当前时间 距离Date 数据 天数
     * @param args
     */
    /**
     * 计算给定日期与当前时间的相差天数
     *
     * @param targetDate 目标日期（如果为 null，默认返回 0）
     * @return 相差天数（正数：目标日期在未来；负数：目标日期在过去）
     */
    public static Integer daysBetweenNow(Date targetDate) {

        if (targetDate == null) {
            return 0;
        }

        long days = DateUtil.betweenDay(targetDate,DateUtil.date(), true);

        // 安全转为 Integer（处理溢出）
        if (days > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (days < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else {
            return (int) days;
        }
    }

    /**
     * 根据时间戳 - 生成业务ID
     */
    public static String generateShortUniqueId() {
        long timestamp = System.currentTimeMillis(); // 毫秒级时间戳
        int random = (int)(Math.random() * 9000) + 1000; // 4位随机数
        return Long.toString(timestamp, 36) + Integer.toString(random, 36);
    }

    private static LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) return null;
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * 格式化时间差，显示 X分钟前、X小时前、X天前、X月前、X年前
     */
    public static String formatTimeAgo(Date updateTime) {
        if (updateTime == null) return "";

        LocalDateTime updateDateTime = convertToLocalDateTime(updateTime);
        LocalDateTime now = LocalDateTime.now();

        long years = ChronoUnit.YEARS.between(updateDateTime, now);
        if (years >= 1) {
            return years + "年前";
        }

        long months = ChronoUnit.MONTHS.between(updateDateTime, now);
        if (months >= 1) {
            return months + "个月前";
        }

        long days = ChronoUnit.DAYS.between(updateDateTime, now);
        if (days >= 1) {
            return days + "天前";
        }

        long hours = ChronoUnit.HOURS.between(updateDateTime, now);
        if (hours >= 1) {
            return hours + "小时前";
        }

        long minutes = ChronoUnit.MINUTES.between(updateDateTime, now);
        if (minutes >= 1) {
            return minutes + "分钟前";
        }

        return "刚刚";
    }

    /**
     * 清洗 AI 返回的 JSON 字符串，修复全角符号和 Markdown 标记
     */
//    public static String sanitize(String rawInput) {
//        if (rawInput == null || rawInput.isBlank()) {
//            return "[]";
//        }
//
//        String json = rawInput.trim();
//
//        // 1. 去除 Markdown 包裹
//        if (json.startsWith("```json")) json = json.substring(7);
//        if (json.startsWith("```")) json = json.substring(3);
//        if (json.endsWith("```")) json = json.substring(0, json.length() - 3);
//
//        // 2. 替换隐形字符 (NBSP) 和 换行符带来的混乱
//        // 先把所有隐形空格换成普通空格
//        json = json.replace("\u00A0", " ");
//
//        // 3. 修复中文全角符号
//        json = json.replace("“", "\"")
//                .replace("”", "\"")
//                .replace("：", ":")
//                .replace("，", ",")
//                .replace("【", "[")
//                .replace("】", "]");
//
//        // 4. 修复嵌套引号幻觉 (例如 "'key'" -> "key")
//        json = json.replace("\"'", "\"").replace("'\"", "\"");
//
//        // =======================================================
//        // 5. 【关键修复】处理多余的逗号 (Double Comma fix)
//        // =======================================================
//
//        // 5.1 合并连续的逗号（例如 "a":"b", ,"c":"d" -> "a":"b", "c":"d"）
//        // 正则含义：匹配一个逗号，后面跟任意空白(包括换行)，再跟一个逗号
//        json = json.replaceAll(",\\s*,", ",");
//
//        // 5.2 去除对象末尾多余的逗号（Trailing Comma fix）
//        // 例如 { "a":1, } -> { "a":1 }
//        json = json.replaceAll(",\\s*}", "}");
//
//        // 5.3 去除数组末尾多余的逗号
//        // 例如 [ {...}, ] -> [ {...} ]
//        json = json.replaceAll(",\\s*]", "]");
//
//        return json.trim();
//    }

    private static final Pattern ESCAPED_QUOTE_PATTERN = Pattern.compile("\\\\\"");
    private static final Pattern MISSING_COLON_PATTERN = Pattern.compile("(\"[a-zA-Z0-9_]+\")\\s*\"");

    public static String sanitize(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return "[]";
        }

        String json = rawInput.trim();

        // 1. 去除 Markdown 包裹
        json = json.replaceAll("```json|```", "").trim();

        // 2. 替换隐形字符 (NBSP)
        json = json.replace("\u00A0", " ");

        // 3. 修复中文全角符号
        json = json.replace("“", "\"").replace("”", "\"")
                .replace("：", ":").replace("，", ",")
                .replace("【", "[").replace("】", "]");

        // 4. 修复嵌套单引号幻觉 (例如 "'key'" -> "key")
        json = json.replace("\"'", "\"").replace("'\"", "\"");

        // =======================================================
        // 5. 【核心修复】修复过度转义和结构性错误 (AI本次报错点)
        // =======================================================

        // 5.1 移除所有结构性的反斜杠 (例如: \"key\" -> "key")
        // 注意：这也会移除描述中不必要的转义，但保留必要的 (如换行符)
        json = ESCAPED_QUOTE_PATTERN.matcher(json).replaceAll("\"");

        // 5.2 修复缺少冒号的错误 (例如: "key" "value" -> "key": "value")
        // 匹配 "key"后面跟着空白和 "value"，在中间插入冒号和空格
        // Group 1 是 key 的双引号部分，后面是 "value"
        json = json.replaceAll("([a-zA-Z0-9_\"]+)\"\\s*\"", "$1\": \"");
        json = json.replaceAll("\"\\s*([a-zA-Z0-9_]+)\\s*\"", "\"$1\"");
        json = json.replaceAll("\\s+", " ").trim();
        // 新增 5.3 修复字面上的 \n 结构性换行符
        // AI 在字段之间输出了字面上的 \n 字符串，必须移除，但要保留逗号
        json = json.replace(",\\n\"", ",\""); // 修复 , \n "key" 模式
        json = json.replace("\\n,", ",");      // 修复 \n , 模式
        json = json.replace("\\n", "");        // 兜底，移除所有剩余的结构性 \n 字符串


        // 6. 处理多余的逗号 (Double Comma fix)
        json = json.replaceAll(",\\s*,", ","); // 合并连续逗号
        json = json.replaceAll(",\\s*}", "}"); // 去除对象末尾逗号
        json = json.replaceAll(",\\s*]", "]"); // 去除数组末尾逗号

        return json.trim();
    }

    public static void main(String[] args) throws InterruptedException {
        Date now = new Date();

        System.out.println(formatTimeAgo(new Date(now.getTime() - 45 * 60 * 1000))); // 45分钟前
        System.out.println(formatTimeAgo(new Date(now.getTime() - 3 * 60 * 60 * 1000))); // 3小时前
        System.out.println(formatTimeAgo(new Date(now.getTime() - 5L * 24 * 3600 * 1000))); // 5天前
        System.out.println(formatTimeAgo(Date.from(LocalDateTime.now().minusMonths(4)
                .atZone(ZoneId.systemDefault()).toInstant()))); // 4月前
        System.out.println(formatTimeAgo(Date.from(LocalDateTime.now().minusYears(2)
                .atZone(ZoneId.systemDefault()).toInstant()))); // 2年前
        System.out.println(formatTimeAgo(new Date())); // 刚刚
    }

    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final DecimalFormat df = new DecimalFormat("#.##");


    public static String getSingleSegmentPromptString(SegmentInfo segment, String imageDescription) {
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
        stringMessage.append("   图片描述：").append(imageDescription).append("\n");
        stringMessage.append("   请注意：确保图像中的元素（如人物服饰、光影、构图等）在分片中得以忠实呈现，保持画面的一致性和连贯性。\n");

        // 镜头切换连贯性
        stringMessage.append("🎬 镜头切换流畅性：特别注意分片首帧的镜头切换与前一分片的流畅过渡，保持视觉上的连续性。\n");
        stringMessage.append("🎥 运镜注意：与前后分片镜头保持一致，避免出现突兀的过渡，确保镜头平滑衔接。\n");
        stringMessage.append("📌 连接前后分片：确保与前后分片的镜头、动作、氛围等有良好的衔接，避免出现突兀的过渡。\n");

        stringMessage.append("---------------------------------------\n");
        stringMessage.append("请根据以上技术要求和描述，生成该分片的视频，并确保其在电影中的整体一致性与流畅性。");

        return stringMessage.toString();
    }


    /**
     * 根据文件大小自动转换为最合适的单位
     * @param file 要计算的文件
     * @return 带有单位的文件大小字符串
     */
    public static String getAutoFileSize(File file) {
        long sizeInBytes = file.length();
        return formatSize(sizeInBytes);
    }

    /**
     * 根据字节大小自动转换为最合适的单位
     * @param sizeInBytes 文件大小(字节)
     * @return 带有单位的文件大小字符串
     */
    public static String formatSize(long sizeInBytes) {
        if (sizeInBytes < KB) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < MB) {
            return df.format((double) sizeInBytes / KB) + " KB";
        } else if (sizeInBytes < GB) {
            return df.format((double) sizeInBytes / MB) + " MB";
        } else {
            return df.format((double) sizeInBytes / GB) + " GB";
        }
    }

    /**
     * 转换为指定单位的大小
     * @param sizeInBytes 文件大小(字节)
     * @param unit 单位(B, KB, MB, GB)
     * @return 转换后的数值
     */
    public static double convertToSpecificUnit(long sizeInBytes, String unit) {
        switch (unit.toUpperCase()) {
            case "KB":
                return (double) sizeInBytes / KB;
            case "MB":
                return (double) sizeInBytes / MB;
            case "GB":
                return (double) sizeInBytes / GB;
            default: // 默认返回字节
                return sizeInBytes;
        }
    }

    /**
     * 转换为指定单位的大小字符串
     * @param sizeInBytes 文件大小(字节)
     * @param unit 单位(B, KB, MB, GB)
     * @return 带有单位的文件大小字符串
     */
    public static String convertToSpecificUnitString(long sizeInBytes, String unit) {
        return df.format(convertToSpecificUnit(sizeInBytes, unit)) + " " + unit;
    }

}
