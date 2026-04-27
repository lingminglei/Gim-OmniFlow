package org.lml.thirdService.AiGateWay;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.lml.AiGateWay.api.AiGatewayRpcService;
import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.req.GatewayRequest;
import org.springframework.stereotype.Component;

/**
 * AI 网关兼容门面。
 * 业务代码继续注入这个类，内部通过 Dubbo 调用独立的 AI 网关服务。
 */
@Component
@Slf4j
public class SmartAIGateway {

    @DubboReference(check = false, timeout = 5000, retries = 0, group = "ai-gateway", version = "1.0.0")
    private AiGatewayRpcService aiGatewayRpcService;

    /**
     * 发起 AI 网关创建类任务。
     */
    public ApiResult<String> generateVideo(GatewayRequest gatewayRequest) {
        try {
            return aiGatewayRpcService.generateVideo(gatewayRequest);
        } catch (Exception e) {
            log.error("调用 AI 网关创建任务失败", e);
            return ApiResult.error("503", "AI网关服务暂不可用");
        }
    }

    /**
     * 查询 AI 网关任务结果。
     */
    public ApiResult<String> queryResult(GatewayRequest gatewayRequest) {
        try {
            return aiGatewayRpcService.queryResult(gatewayRequest);
        } catch (Exception e) {
            log.error("调用 AI 网关查询任务失败", e);
            return ApiResult.error("503", "AI网关服务暂不可用");
        }
    }
}
