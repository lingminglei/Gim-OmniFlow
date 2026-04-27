package org.lml.AiGateWay.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * AI 网关统一请求参数。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GatewayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型。 */
    private String bizType;

    /** 供应商标识。 */
    private String provider;

    /** 动态请求参数。 */
    private Map<String, Object> params;
}
