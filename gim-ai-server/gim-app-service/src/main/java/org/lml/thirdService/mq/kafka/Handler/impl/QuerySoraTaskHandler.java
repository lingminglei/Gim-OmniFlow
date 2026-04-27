//package org.lml.thirdService.mq.kafka.Handler.impl;
//
//import cn.hutool.core.date.DateUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.lml.common.enums.TaskState;
//import org.lml.common.exection.BizException;
//import org.lml.thirdService.mq.kafka.Handler.MessageHandler;
//import org.lml.thirdService.mq.enums.MqBusinessEnum;
//import org.lml.thirdService.mq.entity.BaseMessage;
//import org.lml.service.ai.IFileVideoAssociationService;
//import org.lml.thirdService.AiGateWay.SmartAIGateway;
//import org.lml.thirdService.AiGateWay.common.ApiResult;
//import org.lml.thirdService.AiGateWay.enums.VideoProvider;
//import org.lml.thirdService.AiGateWay.req.GatewayRequest;
//import org.lml.thirdService.generateVideo.sora.GenerateVideoApi;
//import org.lml.utils.CommonUtils;
//import org.lml.utils.RedisUtils;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.lml.common.constant.RedisCacheConstant.PICTURE_RESPONSE_KEY;
//
//@Component
//@Slf4j
//public class QuerySoraTaskHandler implements MessageHandler<String> {
//
//    @Resource
//    private RedisUtils redisUtils;
//
//    @Resource
//    private GenerateVideoApi generateVideoApi;
//
//    @Resource
//    private IFileVideoAssociationService iFileVideoAssociationService;
//
//    @Resource
//    private SmartAIGateway smartAIGateway;
//
//    @Resource
//    private CommonUtils commonUtils;
//
//    /**
//     * Redis 查询信息：HGETALL biz:task:info:a5c1840832ef4da0841d55289185426b
//     * @param message
//     */
//
//    @Override
//    public void handle(BaseMessage<String> message) throws BizException {
//
//        log.info("处理：QUERY_IMAGE_RESULT 业务.");
//        String localId = message.getBusinessKey();
//
//        String key = PICTURE_RESPONSE_KEY + localId;
//
//        String remoteId = (String) redisUtils.hget(key, "taskId");
//
//        if(StringUtils.isBlank(remoteId)){
//            log.info("任务taskId 为空，任务执行失败！");
//            return;
//        }else{
//            //先更新任务状态,验证此时状态,只有状态处于,初始化状态时，才会更新状态
//            Object status = (String)redisUtils.hget(key, "status");
//            if(status.equals(TaskState.INIT.getCode())){
//                redisUtils.hset(key, "status", TaskState.PROCESSING.getCode());
//                redisUtils.hset(key, "updateTime", DateUtil.now());
//            }
//        }
//
//        //改造为: 调用模型接口，统一从AI网关入口
//        GatewayRequest gatewayRequest = new GatewayRequest();
//        gatewayRequest.setBizType(VideoProvider.SORA_TO_VIDEO.getCode());//Sora视频生成
//        Map<String, Object> params = new HashMap<>();
//        params.put("taskId",remoteId);
//        gatewayRequest.setParams(params);
//        ApiResult aiResult = smartAIGateway.queryResult(gatewayRequest);
//
//        log.info("处理结果：{}",aiResult);
//
//        if(aiResult.getCode().equals("200")){
//            log.info("执行成功");
//            //获取任务TaskId
//            String taskState = aiResult.getData().toString();
//
//            log.info("任务生成成功!,文件地址为：{}",taskState);
//
//            redisUtils.hset(key,"result",taskState);
//
//            //状态校验，验证不通过，抛出异常
//            commonUtils.transitState(key,TaskState.SUCCESS);;
//            log.info("【下载任务开始】: taskId={}", remoteId);
//            iFileVideoAssociationService.downloadImage(taskState);
//
//        }else if(aiResult.getCode().equals("202")){
//            log.info("任务正在处理.");
//            //TODO: 状态流转验证
//            commonUtils.transitState(key,TaskState.PROCESSING);
//        }else if(aiResult.getCode().equals("500")){
//            log.info("任务处理失败.");
//            //TODO: 状态流转验证
//            commonUtils.transitState(key,TaskState.FAIL);
//        }else{
//            log.info("异常响应码，处理为异常信息.");
//            //TODO: 状态流转验证
//            commonUtils.transitState(key,TaskState.FAIL);
//        }
//
//    }
//
//    @Override
//    public String getSupportedType() { return MqBusinessEnum.QUERY_TEXT_TO_VIDEO.getCode(); }
//}
