package org.lml.controller.ai;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.lml.AiService.factory.AiModelFactoryService;
import org.lml.AiService.service.AisServices;
import org.lml.billing.api.BillingFacadeService;
import org.lml.billing.enums.BillingOperationStatus;
import org.lml.billing.req.BillingTryDebitReq;
import org.lml.billing.resp.BillingTccResp;
import org.lml.common.enums.TaskState;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.ai.SegmentInfo;
import org.lml.entity.dto.ai.UserVideo;
import org.lml.entity.req.ai.CreatePromptReq;
import org.lml.entity.req.ai.CreateVideoReq;
import org.lml.thirdService.mq.enums.MqBusinessEnum;
import org.lml.thirdService.mq.entity.BaseMessage;
import org.lml.service.ai.IFileVideoAssociationService;
import org.lml.service.ai.IUserVideoService;
import org.lml.thirdService.mq.producer.StreamProducer;
import org.lml.thirdService.mq.streamRocketMq.entity.request.CreateImageRequest;
import org.lml.utils.CommonUtils;
import org.lml.utils.RedisUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.lml.common.constant.CommonConstant.TO_IMAGE;
import static org.lml.common.constant.RedisCacheConstant.PICTURE_RESPONSE_KEY;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lml
 * @since 2025-11-21
 */
@RestController
@RequestMapping("/ai/video")
@Slf4j
public class FileVideoAssociationController {

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private IFileVideoAssociationService iFileVideoAssociationService;

    @Value("${ufop.local-storage-path}")
    private String filePath;

    @Resource
    private IUserVideoService iUserVideoService;

    @Resource
    private AiModelFactoryService aiModelFactoryService;

    @Autowired
    private StreamProducer streamProducer;

    @DubboReference(check = false, timeout = 5000, retries = 0, version = "1.0.0")
    private BillingFacadeService billingFacadeService;

    /**
     * 文生图生成视频 （生成分片镜头)
     */
    @PostMapping("/createVideoBySlice")
    public CommonResult<String> createVideoBySlice(@RequestBody CreateVideoReq createVideoReq) throws InterruptedException {

        List<SegmentInfo> segmentInfoList = createVideoReq.getSegmentInfoList();

        if(CollectionUtils.isEmpty(segmentInfoList)){
            return CommonResult.errorResponse("操作失败，分片信息为空！");
        }

        String videoId = IdUtil.randomUUID();

        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始生成视频: videoId={}", videoId);
//                iFileVideoAssociationService.generatorVideo(videoId,segmentInfoList);

            } catch (Exception e) {
                log.error("视频生成失败: videoId={}", videoId, e);
                UserVideo userVideo = new UserVideo();
                userVideo.setCreator("");
                userVideo.setVideoTitle(videoId);
                StringBuffer errMessage = new StringBuffer();
                errMessage.append("视频生成失败！错误信息："+e.getMessage());
                userVideo.setRemarks(errMessage.toString());
                userVideo.setIsActive(false);
                iUserVideoService.save(userVideo);
            }
        });

       return CommonResult.successResponse(videoId,"操作成功！");

    }

    /**
     * V2：文生图 通过mq 解构业务
     */
    @PostMapping("/textToImageV2")
    public CommonResult<String> textToImageV2(@RequestBody CreateImageRequest createImageRequest) throws InterruptedException {

        try {
            log.info("调用生图 [textToImageV2] 接口. 请求参数：{}",createImageRequest);

            //1、做积分预扣，判断是否用户积分是否充足
            try{
                BillingTryDebitReq billingTryDebitReq = this.buildBillingTryDebitReq();
                CommonResult<BillingTccResp> result = billingFacadeService.tryDebit(billingTryDebitReq);

                if(result.getCode()!=0 || result.getData().getStatus().equals(BillingOperationStatus.SUCCESS)){
                    return CommonResult.errorResponse("积分不足，操作失败！");
                }
            } catch (Exception e){
                return CommonResult.errorResponse("积分不足，操作失败！");
            }

            log.info("积分预扣成功，积分足够操作！！！");

            return CommonResult.successResponse();

//            // 1. 设置平台业务唯一任务.
//            String taskId = IdUtil.fastSimpleUUID();
//
//            //初始化任务执行初始状态
//            String key = PICTURE_RESPONSE_KEY + taskId;
//            Map<String, Object> taskMap = new HashMap<>();
//            taskMap.put("taskId", "");
//            taskMap.put("status", TaskState.INIT.getCode());
//            taskMap.put("updateTime", DateUtil.now());
//            redisUtils.hmset(key, taskMap, 24 * 3600);
//
//            Object currentStatus = redisUtils.hget(key, "status");
//            log.info("Redis 写入验证成功，当前状态: key={},{}", key,currentStatus);
//
//            // 构建 MQ 请求消息体
//            BaseMessage<CreateImageRequest> msg = BaseMessage.<CreateImageRequest>builder()
//                    .messageId(UUID.randomUUID().toString())
//                    .businessKey(taskId)
//                    .source(MqBusinessEnum.TEXT_TO_IMAGE.getCode())
//                    .data(createImageRequest)
//                    .build();
//
//            log.info("[发送链路监控] 准备发送任务，taskId: {}, messageId: {}, 业务源: {}",
//                    taskId, msg.getMessageId(), msg.getSource());
//
//            try {
//                boolean result = streamProducer.send("imageGenerationTask-in-0",
//                        null,
//                        JSON.toJSONString(msg));
//
//                if (result) {
//                    log.info("[发送链路监控] 消息投递成功! taskId: {}, messageId: {}",
//                            taskId, msg.getMessageId());
//                } else {
//                    log.error("[发送链路监控] 消息投递失败 (返回False)! taskId: {}, messageId: {}",
//                            taskId, msg.getMessageId());
//                    throw new RuntimeException("消息发送异常：发送方返回False");
//                }
//            } catch (Exception e) {
//                log.error("[发送链路监控] 发送过程中捕获异常! taskId: {}, messageId: {}, 异常信息: {}",
//                        taskId, msg.getMessageId(), e.getMessage(), e);
//                throw e;
//            }
//
//
//            if(!ObjectUtil.isNull(taskId)){
//                return CommonResult.successResponse(taskId,"操作成功！");
//            }else{
//                return CommonResult.errorResponse("操作失败！");
//            }
        } catch (Exception e) {
            log.error("图片生成失败:", e);

            return CommonResult.errorResponse("操作失败！");
        }
    }

    private BillingTryDebitReq buildBillingTryDebitReq(){

        String tccNo = String.valueOf(IdUtil.getSnowflake().nextId());

        BillingTryDebitReq billingTryDebitReq = new BillingTryDebitReq();

        billingTryDebitReq.setTccTransactionNo(tccNo);//TCC 事务号
        billingTryDebitReq.setExpireSeconds(60*10);//TCC 过期秒数


        billingTryDebitReq.setRequestNo(tccNo);//幂等请求号
        billingTryDebitReq.setUserId(String.valueOf(StpUtil.getLoginId()));//用户ID
        billingTryDebitReq.setBizNo(tccNo);//业务单号
        billingTryDebitReq.setSourceSystem("GIM-APP-SERVICE");//来源系统
        billingTryDebitReq.setAmount(TO_IMAGE);//变更积分数量
        billingTryDebitReq.setRemark("文生图积分预扣");//备注信息

        return billingTryDebitReq;
    }

    /**
     * V2： 查询文生图结果
     */
    @GetMapping("/queryResultTextToImageV2")
    private CommonResult<String> queryResultTextToImageV2(@RequestParam(value = "taskId", required = false) String taskId) {

        log.info("taskId==={}", taskId);

        // 1. 参数校验
        if (ObjectUtil.isNull(taskId) || StringUtils.isBlank(taskId)) {
            return CommonResult.errorResponse("操作失败，taskID为空");
        }

        String lockKey = "LOCK:QUERY_TRIGGER_TEXT_TO_IMAGE:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3. 尝试获取锁：等待 0 秒，租约时间 5 秒。
            boolean isLocked = lock.tryLock(2, 30, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("taskId={} 请求过于频繁，已拦截 (Redisson 锁未获取)", taskId);
                return CommonResult.errorResponse("查询处理中，请勿频繁请求");
            }

            log.info("taskId={}", taskId);
            String key = PICTURE_RESPONSE_KEY + taskId;
            Map<Object, Object> resultMap = redisUtils.hmget(key);

            if(ObjectUtil.isNull(resultMap) || ObjectUtil.isNull(resultMap.get("status"))){
                log.info("任务执行失败,获取任务执行结果失败！");
                return CommonResult.successResponse("3","任务执行失败,获取任务执行结果失败.");
            }

            String status = resultMap.get("status").toString();
            if (TaskState.PROCESSING.getCode().equals(status) || TaskState.SUBMITTED.getCode().equals(status)
            || TaskState.INIT.getCode().equals(status)) {
                log.info("任务正在处理中，请稍后进行尝试！");

                // 基于 updateTime 的频率控制逻辑，距离上次更新状态超过5s 就进行查询。---
                Object lastUpdateTimeObj = resultMap.get("updateTime");
                if (ObjectUtil.isNotNull(lastUpdateTimeObj)) {
                    String lastUpdateTimeStr = lastUpdateTimeObj.toString();
                    // 使用 Hutool 的 DateUtil 计算时间差
                    long between = DateUtil.between(DateUtil.parse(lastUpdateTimeStr), DateUtil.date(), DateUnit.SECOND);

                    // 如果距离上次更新不足 5 秒，则只返回状态，不重复触发 Kafka 补偿查询
                    if (between < 5) {
                        log.info("任务 {} 上次更新在 {} 秒前，跳过 MQ 补偿发送", taskId, between);
                        return CommonResult.successResponse("1", "任务正在处理中，请耐心等待...");
                    }
                }

                log.info("任务 {} 超过 5 秒未更新状态，触发 MQ 补偿查询", taskId);
                //==================================================================
                // 1. 构建消息体
                BaseMessage<CreateImageRequest> msg = BaseMessage.<CreateImageRequest>builder()
                        .messageId(UUID.randomUUID().toString())
                        .businessKey(taskId)
                        .source(MqBusinessEnum.QUERY_IMAGE_RESULT.getCode())
                        .build();

                // 2. 增加结构化调试日志
                String tag = MqBusinessEnum.QUERY_IMAGE_RESULT.getCode();
                log.info("[AI-Task-Query] 准备发起查询请求 | taskId: {}, messageId: {}, tag: {}, targetBinding: aiCreativeTaskQuery-in-0",
                        taskId, msg.getMessageId(), tag);

                try {
                    // 3. 执行发送
                    boolean result = streamProducer.send("aiCreativeTaskQuery-in-0", tag, JSON.toJSONString(msg));

                    // 4. 结果校验与分级日志
                    if (result) {
                        log.info("[AI-Task-Query] 消息投递至 RocketMQ 成功 | taskId: {}, messageId: {}",
                                taskId, msg.getMessageId());
                    } else {
                        log.error("[AI-Task-Query] 消息投递被拒绝 (Producer返回False) | taskId: {}, messageId: {}",
                                taskId, msg.getMessageId());
                        throw new RuntimeException("消息发送异常：Producer 返回 False");
                    }
                } catch (Exception e) {
                    // 5. 异常拦截与上下文记录
                    log.error("[AI-Task-Query] 发送查询请求捕获异常 | taskId: {}, messageId: {}, error: {}",
                            taskId, msg.getMessageId(), e.getMessage(), e);
                    throw e; // 继续向上抛出，触发外层业务的异常处理机制
                }
                //==================================================================

                // 发送完消息后，更新一下 Redis 里的 updateTime
                redisUtils.hset(key, "updateTime", DateUtil.now());

                return CommonResult.successResponse("1","任务正在处理中，请稍后进行尝试！");
            }else if(TaskState.SUCCESS.getCode().equals(status)){
                log.info("任务处理成功！");
                String result = resultMap.get("result").toString();
                return CommonResult.successResponse(result,"操作成功，任务已最终完成！");
            }else if(TaskState.FAIL.getCode().equals(status)){
                log.info("任务处理失败");
                return CommonResult.successResponse("3","任务执行失败！");
            }

            return null;

        } catch (InterruptedException e) {
            // 处理线程中断异常
            Thread.currentThread().interrupt();
            log.error("Redisson 锁尝试获取被中断, taskId={}", taskId, e);
            return CommonResult.errorResponse("系统处理中断");
        } catch (Exception e) {
            log.error("查询视频结果异常, taskId={}", taskId, e);
            return CommonResult.errorResponse("系统异常");
        } finally {
            // 4. 释放锁：确保锁被释放，以便后续轮询能继续进行
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("taskId={} 查询锁已释放", taskId);
            }
        }
    }


    /**
     * V2 文生图生成视频 改造通过mq 解构业务
     * @param createVideoReq
     * @return
     */
    @PostMapping("/createVideoV2")
    public CommonResult<String> createVideoV2(@RequestBody CreateVideoReq createVideoReq){

        if(StringUtils.isBlank(createVideoReq.getPrompt())){
            return CommonResult.errorResponse("操作失败，分片信息为空！");
        }

        try {
            // 1. 确保业务请求中有唯一标识
            String taskId = IdUtil.fastSimpleUUID();

            //初始化任务执行初始状态
            String key = PICTURE_RESPONSE_KEY+taskId;
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("taskId", "");
            taskMap.put("status", TaskState.INIT.getCode());
            taskMap.put("updateTime", DateUtil.now());
            redisUtils.hmset(key, taskMap, 24 * 3600);

            Object currentStatus = redisUtils.hget(key, "status");
            log.info("Redis 写入验证成功，当前状态: key={},{}", key,currentStatus);

            BaseMessage<CreateVideoReq> msg = BaseMessage.<CreateVideoReq>builder()
                    .messageId(UUID.randomUUID().toString())
                    .businessKey(taskId)
                    .source(MqBusinessEnum.TEXT_TO_VIDEO.getCode())
                    .data(createVideoReq)
                    .build();

            // 2. 增加详细的调试日志
            log.info("[发送链路监控] 准备发送任务，taskId: {}, messageId: {}, 业务源: {}",
                    taskId, msg.getMessageId(), msg.getSource());

            try {
                boolean result = streamProducer.send("soraCreativeTask-in-0",
                        null,
                        JSON.toJSONString(msg));

                if (result) {
                    log.info("[发送链路监控] 消息投递成功! taskId: {}, messageId: {}",
                            taskId, msg.getMessageId());
                } else {
                    log.error("[发送链路监控] 消息投递失败 (返回False)! taskId: {}, messageId: {}",
                            taskId, msg.getMessageId());
                    throw new RuntimeException("消息发送异常：发送方返回False");
                }
            } catch (Exception e) {
                log.error("[发送链路监控] 发送过程中捕获异常! taskId: {}, messageId: {}, 异常信息: {}",
                        taskId, msg.getMessageId(), e.getMessage(), e);
                throw e;
            }


            if(!ObjectUtil.isNull(taskId)){
                return CommonResult.successResponse(taskId,"操作成功！");
            }else{
                return CommonResult.errorResponse("操作失败！");
            }
        } catch (Exception e) {
            log.error("视频生成失败:", e);

            return CommonResult.errorResponse("操作失败！");
        }

    }

    /**
     * 查询视频生成结果接口，使用 Redisson 锁防止轮询并发击穿。
     * * @param taskId 任务ID
     * @return 结果状态
     */
    @GetMapping("/queryResult")
    private CommonResult<Map<String,String>> queryVideoResult(@RequestParam(value = "taskId", required = false) String taskId) {

        log.info("taskId==={}", taskId);

        // 1. 参数校验
        if (ObjectUtil.isNull(taskId) || StringUtils.isBlank(taskId)) {
            return CommonResult.errorResponse("操作失败，taskID为空");
        }

        String lockKey = "LOCK:QUERY_TRIGGER:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3. 尝试获取锁：等待 0 秒，租约时间 5 秒。
            boolean isLocked = lock.tryLock(2, 30,TimeUnit.SECONDS);

            if (!isLocked) {
                Map<String,String> map = new HashMap<>();
                map.put("result","0");
                map.put("message","查询处理中，请勿频繁请求");
                log.warn("taskId={} 请求过于频繁，已拦截 (Redisson 锁未获取)", taskId);
                return CommonResult.successResponse(map);
            }

            log.info("taskId={}",taskId);
            // ：检查任务是否已最终完成 (SUCCESS 状态)
            if (redisUtils.hasKey(taskId)) {
                log.info("taskId={} 任务已完成，跳过外部API调用，直接返回成功状态", taskId);
                Map<String,String> map = new HashMap<>();
                map.put("result","0");
                map.put("message","任务已完成，跳过外部API调用，直接返回成功状态");
                // 这里返回一个明确的成功信息，告知前端任务已结束
                return CommonResult.successResponse(map);
            }

//            String videoPath = generateVideoApi.quereyVideo(taskId);
            String videoPath = "";

            if (ObjectUtil.isNull(videoPath)) {
                Map<String,String> map = new HashMap<>();
                map.put("result","-1");
                map.put("message","视频生成失败");
                return CommonResult.successResponse(map);
            }

            if ("2".equals(videoPath) || "3".equals(videoPath)) {
                // 状态码 0, 2, 3 表示：处理中、失败等，直接返回状态
                Map<String,String> map = new HashMap<>();
                map.put("result","-1");
                map.put("message","生成失败！");
                return CommonResult.successResponse(videoPath);
            } else if("0".equals(videoPath)){
                Map<String,String> map = new HashMap<>();
                map.put("result","0");
                map.put("message","还在生成，请稍后尝试！");
                return CommonResult.successResponse(map);
            } else {
                // 触发异步下载和去重任务。
                log.info("【下载任务开始】: taskId={}", taskId);
                iFileVideoAssociationService.downloadVideo(videoPath);

                // 2. 标记任务为永久完成，防止后续轮询再次进入这段成功逻辑
                redisUtils.set(taskId,1000); // 假设您已经实现了这个方法

                Map<String,String> map = new HashMap<>();
                map.put("result","1");
                map.put("filePath",videoPath);
                map.put("message","操作成功，下载任务已在后台启动！");
                return CommonResult.successResponse(map);
            }

        } catch (InterruptedException e) {
            // 处理线程中断异常
            Thread.currentThread().interrupt();
            log.error("Redisson 锁尝试获取被中断, taskId={}", taskId, e);
            Map<String,String> map = new HashMap<>();
            map.put("result","0");
            map.put("message","系统处理中断");
            return CommonResult.successResponse(map);
        } catch (Exception e) {
            log.error("查询视频结果异常, taskId={}", taskId, e);
            Map<String,String> map = new HashMap<>();
            map.put("result","0");
            map.put("message","查询视频结果异常");
            return CommonResult.successResponse(map);
        } finally {
            // 4. 释放锁：确保锁被释放，以便后续轮询能继续进行
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("taskId={} 查询锁已释放", taskId);
            }
        }
    }

    /**
     * 生成提示词
     */
    @PostMapping("/createPrompt")
    public CommonResult<List<SegmentInfo>> createPrompt(@RequestBody CreatePromptReq createPromptReq){

        OpenAiChatModel openAiChatModelJson = aiModelFactoryService.processWithOpenAi();

        AisServices assistant = AiServices.builder(AisServices.class)
                .chatModel(openAiChatModelJson)
                .build();

        String title = createPromptReq.getTitle();
        String userPrompt = createPromptReq.getUserPrompt();
        String videoStyle = createPromptReq.getVideoStyle();
        Integer videoLength = createPromptReq.getVideoLength();

        String jsonList = assistant.chatGetPromtSlice(title,userPrompt,
                videoStyle,videoLength);

        log.info("处理前：{}",jsonList);

        // 1. 打印原始日志（用于调试）
        System.out.println("Raw AI Output: " + jsonList);

        // 2. 清洗数据
        String cleanJson = CommonUtils.sanitize(jsonList);

        log.info("处理后：{}",cleanJson);

        List<SegmentInfo> segments = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        // 允许特殊控制字符（解决换行符问题）
        mapper.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());

        // 忽略 Java Bean 中不存在的字段（防止 AI 瞎编字段导致报错）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 解析为 List<VideoSegment>
        try {
            segments = mapper.readValue(cleanJson, new TypeReference<List<SegmentInfo>>() {});
            log.info("解析数据成功：segments={}",segments);
        } catch (Exception e){
            log.info("e={}",e.getMessage());
        }

        return CommonResult.successResponse(segments);
    }


    /**
     * 生产分片镜头信息
     */
    @PostMapping("/createPromptV2")
    public CommonResult<List<String>> generotData1(@RequestBody CreatePromptReq createPromptReq){

        OpenAiChatModel openAiChatModelJson = aiModelFactoryService.processWithDeepSeekChat();

        AisServices assistant = AiServices.builder(AisServices.class)
                .chatModel(openAiChatModelJson)
                .build();

        String userPrompt = createPromptReq.getUserPrompt();
        String videoStyle = createPromptReq.getVideoStyle();
        Integer videoLength = createPromptReq.getVideoLength();

        String jsonList = assistant.chatGetPromtSliceV2(userPrompt,videoStyle,videoLength);

        log.info("处理前：{}",jsonList);

        // 1. 打印原始日志（用于调试）
        System.out.println("Raw AI Output: " + jsonList);

        // 2. 清洗数据
        String cleanJson = CommonUtils.sanitize(jsonList);

        log.info("处理后：{}",cleanJson);

        List<String> segments = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        // 允许特殊控制字符（解决换行符问题）
        mapper.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());

        // 忽略 Java Bean 中不存在的字段（防止 AI 瞎编字段导致报错）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 解析为 List<VideoSegment>
        try {
            segments = mapper.readValue(cleanJson, new TypeReference<List<String>>() {});
            log.info("解析数据成功：segments={}",segments);
        } catch (Exception e){
            log.info("e={}",e.getMessage());
        }

        return CommonResult.successResponse(segments);
    }

}
