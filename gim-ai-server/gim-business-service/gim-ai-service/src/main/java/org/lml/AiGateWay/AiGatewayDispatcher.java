package org.lml.AiGateWay;

import lombok.extern.slf4j.Slf4j;
import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.req.GatewayRequest;
import org.lml.AiGateWay.service.VideoStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 网关内部调度器。
 */
@Component
@Slf4j
public class AiGatewayDispatcher {

    private final Map<String, VideoStrategy> strategies = new ConcurrentHashMap<>();

    public AiGatewayDispatcher(List<VideoStrategy> strategyList) {
        strategyList.forEach(strategy -> strategies.put(strategy.getBizType(), strategy));
    }

    /**
     * 分发创建类任务。
     */
    public ApiResult<String> generateVideo(GatewayRequest gatewayRequest) {
        String bizType = gatewayRequest == null ? null : gatewayRequest.getBizType();
        VideoStrategy strategy = strategies.get(bizType);
        if (strategy == null) {
            log.warn("AI 网关未找到对应策略, bizType={}", bizType);
            return ApiResult.taskFail("未找到对应的 AI 网关策略: " + bizType);
        }

        try {
            log.info("AI 网关开始执行创建任务, bizType={}", bizType);
            return strategy.execute(gatewayRequest);
        } catch (Exception e) {
            log.error("AI 网关执行创建任务异常, bizType={}", bizType, e);
            return ApiResult.error("500", "AI 网关执行异常: " + e.getMessage());
        }
    }

    /**
     * 分发查询类任务。
     */
    public ApiResult<String> queryResult(GatewayRequest gatewayRequest) {
        String bizType = gatewayRequest == null ? null : gatewayRequest.getBizType();
        VideoStrategy strategy = strategies.get(bizType);
        if (strategy == null) {
            log.warn("AI 网关未找到查询策略, bizType={}", bizType);
            return ApiResult.taskFail("未找到对应的 AI 网关策略: " + bizType);
        }

        try {
            log.info("AI 网关开始查询任务结果, bizType={}", bizType);
            return strategy.query(gatewayRequest);
        } catch (Exception e) {
            log.error("AI 网关查询任务异常, bizType={}", bizType, e);
            return ApiResult.error("500", "AI 网关查询异常: " + e.getMessage());
        }
    }
}
