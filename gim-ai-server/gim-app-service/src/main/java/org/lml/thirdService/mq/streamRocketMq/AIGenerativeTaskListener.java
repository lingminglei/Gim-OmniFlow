package org.lml.thirdService.mq.streamRocketMq;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.common.enums.TaskState;
import org.lml.entity.req.ai.CreateImageReq;
import org.lml.entity.req.ai.CreateVideoReq;
import org.lml.thirdService.mq.enums.MqBusinessEnum;
import org.lml.thirdService.mq.entity.BaseMessage;
import org.lml.service.ai.IFileVideoAssociationService;
import org.lml.thirdService.AiGateWay.SmartAIGateway;
import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.enums.VideoProvider;
import org.lml.AiGateWay.req.GatewayRequest;
import org.lml.thirdService.mq.entity.MessageBody;
import org.lml.thirdService.mq.producer.StreamProducer;
import org.lml.thirdService.mq.streamRocketMq.entity.request.CreateImageRequest;
import org.lml.utils.CommonUtils;
import org.lml.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.lml.common.constant.RedisCacheConstant.PICTURE_RESPONSE_KEY;
import static org.lml.thirdService.mq.consumer.AbstractStreamConsumer.getMessage;

@Configuration
@Slf4j
public class AIGenerativeTaskListener {

    @Resource
    private RedisUtils redisUtils;
    @Resource
    private SmartAIGateway smartAIGateway;
    @Autowired
    private StreamProducer streamProducer;
    @Resource
    private IFileVideoAssociationService iFileVideoAssociationService;
    @Resource
    private CommonUtils commonUtils;

    /**AIGenerativeTask
     * 定义非事务消费者 Bean
     * 名称必须与配置文件中 spring.cloud.function.definition 的名称一致
     */
    @Bean
    public Consumer<Message<MessageBody>> imageGenerationTask() {
        return message -> {

            log.info("AIGenerativeTaskListener================================");

            BaseMessage<CreateImageRequest> baseMessage = getMessage(message, BaseMessage.class);

            String businessKey = baseMessage.getBusinessKey();
            String key = PICTURE_RESPONSE_KEY + businessKey;

            log.info("[AI生成任务] 收到新请求, businessKey: {}, payload: {}", businessKey, baseMessage.getData());

            // 1. 数据解析与策略选择
            CreateImageReq req = JSONUtil.toBean(JSONUtil.parseObj(baseMessage.getData()), CreateImageReq.class);
            VideoProvider videoProvider = StringUtils.isBlank(req.getFilePath()) ?
                    VideoProvider.COMFY_UI : VideoProvider.COMFY_UI_IMAGE_EDIT;

            log.debug("[AI生成任务] 解析参数完成, businessKey: {}, provider: {}, prompt长度: {}",
                    businessKey, videoProvider, req.getPrompt() != null ? req.getPrompt().length() : 0);

            // 2. 调用 AI 网关
            GatewayRequest gatewayRequest = new GatewayRequest();
            gatewayRequest.setBizType(videoProvider.getCode());
            Map<String, Object> params = new HashMap<>();
            params.put("prompt", req.getPrompt());
            params.put("fileUrl", req.getFilePath());
            gatewayRequest.setParams(params);

            log.info("[AI生成任务] 调用AI网关进行生成, businessKey: {}, provider: {}", businessKey, videoProvider.getCode());
            ApiResult aiResult = smartAIGateway.generateVideo(gatewayRequest);
            log.info("[AI生成任务] AI网关响应, businessKey: {}, code: {}, data: {}", businessKey, aiResult.getCode(), aiResult.getData());

            // 3. 处理业务逻辑
            processResult(key, aiResult, videoProvider, businessKey);
        };
    }

    private void processResult(String key, ApiResult aiResult, VideoProvider videoProvider, String bizKey) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("updateTime", DateUtil.now());

        if ("200".equals(aiResult.getCode())) {
            String remoteTaskId = aiResult.getData().toString();
            updateMap.put("status", TaskState.SUBMITTED.getCode());
            updateMap.put("taskId", remoteTaskId);
            updateMap.put("bizType", videoProvider.getCode());
            redisUtils.hmset(key, updateMap);

            log.info("[AI生成任务] 任务提交成功，准备下发查询指令, bizKey: {}, remoteTaskId: {}", bizKey, remoteTaskId);

            // 发送查询消息
            BaseMessage<CreateImageRequest> msg = BaseMessage.<CreateImageRequest>builder()
                    .messageId(UUID.randomUUID().toString())
                    .businessKey(bizKey) // 注意：这里通常建议保持全局唯一业务ID，若是异步查询需确认用bizKey还是remoteTaskId
                    .source(MqBusinessEnum.QUERY_IMAGE_RESULT.getCode())
                    .build();

            try {
                streamProducer.send("aiCreativeTaskQuery-in-0", null, JSON.toJSONString(msg));
                log.info("[AI生成任务] 成功发送下游查询消息, bizKey: {}", bizKey);
            } catch (Exception e) {
                log.error("[AI生成任务] 发送MQ消息失败, bizKey: {}, error: {}", bizKey, e.getMessage(), e);
                throw e;
            }
        } else {
            String status = "500".equals(aiResult.getCode()) ? TaskState.FAIL.getCode() : TaskState.PROCESSING.getCode();
            updateMap.put("status", status);
            redisUtils.hmset(key, updateMap);
            log.warn("[AI生成任务] AI网关返回非200状态, bizKey: {}, code: {}, 记录状态: {}", bizKey, aiResult.getCode(), status);
        }
    }


    /**
     * 定义 Sora 创建文生视频的
     *
     * todo: 未测试，对照-TextToVideoHandler.java
     */
    @Bean
    public Consumer<Message<MessageBody>> soraCreativeTask() {
        return message -> {

            BaseMessage<CreateVideoReq> baseMessage = getMessage(message, BaseMessage.class);

            CreateVideoReq req = JSONUtil.toBean(JSONUtil.parseObj(baseMessage.getData()), CreateVideoReq.class);

            String key = PICTURE_RESPONSE_KEY + baseMessage.getBusinessKey();

            log.info("[Sora创作] 收到任务请求, businessKey: {}", baseMessage.getBusinessKey());

            GatewayRequest gatewayRequest = new GatewayRequest();
            gatewayRequest.setBizType(VideoProvider.SORA_TO_VIDEO.getCode());
            Map<String, Object> params = new HashMap<>();
            params.put("prompt", req.getPrompt());
            params.put("fileUrl", req.getFilePath());
            gatewayRequest.setParams(params);

            ApiResult aiResult = smartAIGateway.generateVideo(gatewayRequest);
            processGenerationResult(baseMessage, key, aiResult);
        };
    }

    private void processGenerationResult(BaseMessage<CreateVideoReq> message, String key, ApiResult aiResult) {
        log.info("[Sora创作] AI网关返回结果: {}", aiResult);
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("updateTime", DateUtil.now());

        if ("200".equals(aiResult.getCode())) {
            updateMap.put("status", TaskState.SUBMITTED.getCode());
            updateMap.put("taskId", aiResult.getData().toString());
            updateMap.put("bizType", VideoProvider.SORA_TO_VIDEO.getCode());
            redisUtils.hmset(key, updateMap);

            // 通过 StreamBridge 发送后续查询消息
            BaseMessage<CreateImageRequest> msg = BaseMessage.<CreateImageRequest>builder()
                    .messageId(UUID.randomUUID().toString())
                    .businessKey(aiResult.getData().toString())
                    .source(MqBusinessEnum.QUERY_TEXT_TO_VIDEO.getCode())
                    .build();

            log.info("[Sora创作] 提交成功，发送查询任务, taskId: {}", message.getBusinessKey());
            streamProducer.send("soraTaskQueryConsumer-in-0", null, JSON.toJSONString(msg));

        } else {
            updateMap.put("status", TaskState.FAIL.getCode());
            redisUtils.hmset(key, updateMap);
        }
    }

}
