//package org.lml.thirdService.mq.kafka.Handler.impl;
//
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.json.JSONUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.lml.common.enums.TaskState;
//import org.lml.entity.req.ai.CreateImageReq;
//import org.lml.thirdService.mq.kafka.Handler.MessageHandler;
//import org.lml.thirdService.mq.kafka.Sender.MessageSender;
//import org.lml.thirdService.mq.enums.MqBusinessEnum;
//import org.lml.thirdService.mq.entity.BaseMessage;
//import org.lml.service.ai.IFileVideoAssociationService;
//import org.lml.thirdService.AiGateWay.SmartAIGateway;
//import org.lml.thirdService.AiGateWay.common.ApiResult;
//import org.lml.thirdService.AiGateWay.enums.VideoProvider;
//import org.lml.thirdService.AiGateWay.req.GatewayRequest;
//import org.lml.utils.RedisUtils;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.lml.common.constant.RedisCacheConstant.PICTURE_RESPONSE_KEY;
//
///**
// * 文生图业务处理
// */
//@Component
//@Slf4j
//public class TextToImageHandler implements MessageHandler<CreateImageReq> {
//
//    @Resource
//    private RedisUtils redisUtils;
//
//    @Resource
//    private IFileVideoAssociationService iFileVideoAssociationService;
//
//    @Value("${app.kafka.topics.external-api}")
//    private String topic;
//
//    @Resource
//    private MessageSender messageSender;
//
//    @Resource
//    private SmartAIGateway smartAIGateway;
//
//
//    @Override
//    public void handle(BaseMessage<CreateImageReq> message) {
//
//        CreateImageReq req = JSONUtil.toBean(JSONUtil.parseObj(message.getData()), CreateImageReq.class);
//
//        String key = PICTURE_RESPONSE_KEY + message.getBusinessKey();
//
//
//        log.info("处理业务:[文生图业务处理]，请求参数：{}", JSONUtil.toJsonStr(req));
//
//        /**
//         * 在这里通过消费者方式去调用外部API 接口
//         *
//         * 修改为统一从AI 网关，调用模型接口
//         */
//
//        VideoProvider videoProvider = null;
//        if(StringUtils.isBlank(req.getFilePath())){
//            videoProvider = VideoProvider.COMFY_UI;
//        }else{
//            videoProvider = VideoProvider.COMFY_UI_IMAGE_EDIT;
//        }
//
//        GatewayRequest gatewayRequest = new GatewayRequest();
//        gatewayRequest.setBizType(videoProvider.getCode());//Sora视频生成
//        Map<String, Object> params = new HashMap<>();
//        params.put("prompt",req.getPrompt());
//        params.put("fileUrl",req.getFilePath());
//        gatewayRequest.setParams(params);
//
//        ApiResult aiResult = smartAIGateway.generateVideo(gatewayRequest);//文件地址
//
//        Map<String, Object> updateMap = new HashMap<>();
//        updateMap.put("updateTime", DateUtil.now());
//        log.info("处理结果：{}",aiResult);
//        if(aiResult.getCode().equals("200")){
//            log.info("执行成功");
//            //获取任务TaskId
//            String taskId = aiResult.getData().toString();
//            Map<Object, Object> resultMap = redisUtils.hmget(key);
//            log.info("key={},resultMap=={}",key,resultMap);
//            //TODO: 状态流转验证
//            updateMap.put("status", TaskState.SUBMITTED.getCode());
//            updateMap.put("taskId", taskId);
//            updateMap.put("bizType",videoProvider.getCode());//业务类型
//            redisUtils.hmset(key, updateMap);
//
//            // 发送查询消息 (这里可以复用同一个 Topic，但 Source 换成查询)
//            BaseMessage<CreateImageReq> msg = BaseMessage.<CreateImageReq>builder()
//                    .messageId(UUID.randomUUID().toString())
//                    .businessKey(message.getBusinessKey())// 使用 taskId 保证分区负载均衡
//                    .source(MqBusinessEnum.QUERY_IMAGE_RESULT.getCode())// 匹配 Handler 的 getSupportedType()
//                    .build();
////            messageSender.send(topic, msg);
//
//        }else if(aiResult.getCode().equals("202")){
//            log.info("任务正在处理.");
//            //TODO: 状态流转验证
//            updateMap.put("status", TaskState.PROCESSING.getCode());
//            redisUtils.hmset(key, updateMap);
//
//        }else if(aiResult.getCode().equals("500")){
//            log.info("任务处理失败.");
//            //TODO: 状态流转验证
//            updateMap.put("status", TaskState.FAIL.getCode());
//            redisUtils.hmset(key, updateMap);
//        }else{
//            log.info("异常响应码，处理为异常信息.");
//            //TODO: 状态流转验证
//            updateMap.put("status", TaskState.FAIL.getCode());
//            redisUtils.hmset(key, updateMap);
//        }
//
//        Map<Object, Object> resultMap1 = redisUtils.hmget(key);
//        log.info("Redis 写入验证成功，当前状态: key={},{}", resultMap1);
//
//        // 从 Hash 中只获取 status 这一项
//        Object status = redisUtils.hget(key, "status");
//    }
//
//
//    @Override
//    public String getSupportedType() {
//        return MqBusinessEnum.TEXT_TO_IMAGE.getCode();
//    }
//}
