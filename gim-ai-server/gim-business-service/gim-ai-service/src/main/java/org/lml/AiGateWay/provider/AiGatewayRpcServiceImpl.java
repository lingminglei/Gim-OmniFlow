package org.lml.AiGateWay.provider;

import org.apache.dubbo.config.annotation.DubboService;
import org.lml.AiGateWay.AiGatewayDispatcher;
import org.lml.AiGateWay.api.AiGatewayRpcService;
import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.req.GatewayRequest;

/**
 * AI 网关 Dubbo Provider 实现。
 */
@DubboService(group = "ai-gateway", version = "1.0.0", timeout = 5000, retries = 0)
public class AiGatewayRpcServiceImpl implements AiGatewayRpcService {

    private final AiGatewayDispatcher aiGatewayDispatcher;

    public AiGatewayRpcServiceImpl(AiGatewayDispatcher aiGatewayDispatcher) {
        this.aiGatewayDispatcher = aiGatewayDispatcher;
    }

    /**
     * 处理创建类网关请求。
     */
    @Override
    public ApiResult<String> generateVideo(GatewayRequest gatewayRequest) {
        return aiGatewayDispatcher.generateVideo(gatewayRequest);
    }

    /**
     * 处理查询类网关请求。
     */
    @Override
    public ApiResult<String> queryResult(GatewayRequest gatewayRequest) {
        return aiGatewayDispatcher.queryResult(gatewayRequest);
    }
}
