package org.lml.thirdService.mq.streamRocketMq;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.common.enums.TaskState;
import org.lml.common.exection.BizException;
import org.lml.thirdService.mq.entity.BaseMessage;
import org.lml.service.ai.IFileVideoAssociationService;
import org.lml.thirdService.AiGateWay.SmartAIGateway;
import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.enums.VideoProvider;
import org.lml.AiGateWay.req.GatewayRequest;
import org.lml.thirdService.minIO.service.MinioService;
import org.lml.thirdService.mq.entity.MessageBody;
import org.lml.thirdService.mq.streamRocketMq.entity.request.CreateImageRequest;
import org.lml.utils.CommonUtils;
import org.lml.utils.RedisUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.messaging.Message;

import static org.lml.common.constant.RedisCacheConstant.PICTURE_RESPONSE_KEY;
import static org.lml.thirdService.mq.consumer.AbstractStreamConsumer.getMessage;
//import static org.lml.thirdService.mq.consumer.AbstractStreamConsumer.getMessage;

@Configuration
@Slf4j
public class AITaskQueryTaskListener {

    @Resource private RedisUtils redisUtils;
    @Resource private IFileVideoAssociationService iFileVideoAssociationService;
    @Resource private SmartAIGateway smartAIGateway;
    @Resource private CommonUtils commonUtils;
    @Resource private MinioService minioService;

    /**
     * 定义非事务消费者：处理 查询【文生图】任务
     */
    @Bean
    public Consumer<Message<MessageBody>> aiCreativeTaskQuery() {
        return message -> {

            log.info("message==={}",message);

            BaseMessage<CreateImageRequest> baseMessage = getMessage(message, BaseMessage.class);

            String localId = baseMessage.getBusinessKey();

            String key = PICTURE_RESPONSE_KEY + localId;

            log.info("[AI任务轮询] 收到处理请求, businessKey: {}, payload: {}", localId, baseMessage);

            // 1. Redis 信息获取
            String remoteId = (String) redisUtils.hget(key, "taskId");
            String bizType = (String) redisUtils.hget(key, "bizType");
            Object status = redisUtils.hget(key, "status");

            if (StringUtils.isBlank(remoteId)) {
                log.error("[AI任务轮询] 异常：Redis中未找到taskId，无法进行轮询，key: {}", key);
                return;
            }

            log.debug("[AI任务轮询] 当前任务状态: {}, remoteId: {}, bizType: {}", status, remoteId, bizType);

            // 2. 状态预校验与流转
            if (TaskState.INIT.getCode().equals(status)) {
                log.info("[AI任务轮询] 任务状态由INIT变更为PROCESSING, localId: {}", localId);
                redisUtils.hset(key, "status", TaskState.PROCESSING.getCode());
                redisUtils.hset(key, "updateTime", DateUtil.now());
            }

            // 3. 调用 AI 网关
            GatewayRequest gatewayRequest = new GatewayRequest();
            gatewayRequest.setBizType(bizType);
            Map<String, Object> params = new HashMap<>();
            params.put("taskId", remoteId);
            gatewayRequest.setParams(params);

            log.info("[AI任务轮询] 开始调用AI网关查询, remoteId: {}", remoteId);
            ApiResult aiResult = smartAIGateway.queryResult(gatewayRequest);
            log.info("[AI任务轮询] AI网关响应结果: {}", aiResult);

            try {
                processAiResult(key, aiResult, remoteId);
            } catch (Exception e) {
                log.error("[AI任务轮询] 任务处理异常, localId: {}, 错误信息: {}", localId, e.getMessage(), e);
            }
        };
    }

    private void processAiResult(String key, ApiResult aiResult, String remoteId) throws BizException {
        if (aiResult == null) {
            log.error("[AI任务处理] 收到空响应结果, remoteId: {}", remoteId);
            return;
        }

        if ("200".equals(aiResult.getCode())) {
            log.info("[AI任务处理] 任务完成，开始文件上传, remoteId: {}", remoteId);
            String fileUrl = aiResult.getData().toString();
            String myOssUrl = minioService.uploadUrlFile(fileUrl);

            redisUtils.hset(key, "result", myOssUrl);
            commonUtils.transitState(key, TaskState.SUCCESS);
            iFileVideoAssociationService.downloadImage(fileUrl);
            log.info("[AI任务处理] 任务状态更新为SUCCESS, remoteId: {}", remoteId);
        } else if ("202".equals(aiResult.getCode())) {
            log.info("[AI任务处理] 任务处理中，保持原状态, remoteId: {}", remoteId);
            commonUtils.transitState(key, TaskState.PROCESSING);
        } else {
            log.warn("[AI任务处理] 任务执行失败, code: {}, msg: {}, remoteId: {}",
                    aiResult.getCode(), aiResult.getMsg(), remoteId);
            commonUtils.transitState(key, TaskState.FAIL);
        }
    }


    /**
     * 定义非事务消费者：处理 查询【文生视频】任务
     *
     * todo: 未测试，对照-QuerySoraTaskHandler.java
     */
    @Bean
    public Consumer<Message<MessageBody>> soraTaskQueryConsumer() {
        return message -> {
            // 1. 获取基础消息体
            BaseMessage baseMessage = getMessage(message, BaseMessage.class);
            String localId = baseMessage.getBusinessKey();
            String key = PICTURE_RESPONSE_KEY + localId;

            log.info("[Sora任务轮询] 收到处理请求, businessKey: {}, remoteId: {}", localId, localId);

            // 2. Redis 校验
            String remoteId = (String) redisUtils.hget(key, "taskId");
            if (StringUtils.isBlank(remoteId)) {
                log.error("[Sora任务轮询] Redis中未找到taskId, key: {}", key);
                return;
            }

            // 3. 状态检查
            Object status = redisUtils.hget(key, "status");
            if (TaskState.INIT.getCode().equals(status)) {
                redisUtils.hset(key, "status", TaskState.PROCESSING.getCode());
                redisUtils.hset(key, "updateTime", DateUtil.now());
            }

            // 4. 调用 AI 网关 (Sora专有业务)
            GatewayRequest gatewayRequest = new GatewayRequest();
            gatewayRequest.setBizType(VideoProvider.SORA_TO_VIDEO.getCode());
            Map<String, Object> params = new HashMap<>();
            params.put("taskId", remoteId);
            gatewayRequest.setParams(params);

            log.info("[Sora任务轮询] 调用AI网关, taskId: {}", remoteId);
            ApiResult aiResult = smartAIGateway.queryResult(gatewayRequest);

            try {
                processSoraResult(key, aiResult, remoteId);
            } catch (Exception e) {
                log.error("[Sora任务轮询] 任务处理异常, localId: {}, msg: {}", localId, e.getMessage(), e);
            }
        };
    }

    private void processSoraResult(String key, ApiResult aiResult, String remoteId) throws BizException {
        if ("200".equals(aiResult.getCode())) {
            String resultUrl = aiResult.getData().toString();
            log.info("[Sora任务处理] 生成成功, url: {}", resultUrl);
            redisUtils.hset(key, "result", resultUrl);
            commonUtils.transitState(key, TaskState.SUCCESS);
            iFileVideoAssociationService.downloadImage(resultUrl);
        } else if ("202".equals(aiResult.getCode())) {
            commonUtils.transitState(key, TaskState.PROCESSING);
        } else {
            log.warn("[Sora任务处理] 执行失败, code: {}, remoteId: {}", aiResult.getCode(), remoteId);
            commonUtils.transitState(key, TaskState.FAIL);
        }
    }
}
