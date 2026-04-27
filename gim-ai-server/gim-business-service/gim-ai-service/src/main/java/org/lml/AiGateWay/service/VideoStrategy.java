package org.lml.AiGateWay.service;

import org.lml.AiGateWay.common.ApiResult;
import org.lml.AiGateWay.req.GatewayRequest;

/**
 * AI 网关策略接口。
 */
public interface VideoStrategy {

    /**
     * 返回当前策略负责的业务类型编码。
     */
    String getBizType();

    /**
     * 执行创建类任务。
     */
    ApiResult<String> execute(GatewayRequest gatewayRequest);

    /**
     * 查询任务结果。
     */
    ApiResult<String> query(GatewayRequest gatewayRequest);
}
