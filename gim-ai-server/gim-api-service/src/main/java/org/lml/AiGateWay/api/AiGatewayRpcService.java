package org.lml.AiGateWay.api;

import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.req.GatewayRequest;

/**
 * AI 网关 Dubbo 对外服务接口。
 */
public interface AiGatewayRpcService {

    /**
     * 发起 AI 网关创建类任务。
     *
     * @param gatewayRequest 网关请求参数
     * @return 网关执行结果
     */
    ApiResult<String> generateVideo(GatewayRequest gatewayRequest);

    /**
     * 查询 AI 网关任务执行结果。
     *
     * @param gatewayRequest 网关请求参数
     * @return 网关查询结果
     */
    ApiResult<String> queryResult(GatewayRequest gatewayRequest);
}
