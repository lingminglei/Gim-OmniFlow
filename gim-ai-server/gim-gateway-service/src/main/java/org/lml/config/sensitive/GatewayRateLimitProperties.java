package org.lml.config.sensitive;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关限流配置属性。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {

    /**
     * 全局默认每秒请求数。
     */
    private double defaultCount = 120D;

    /**
     * 全局默认时间窗口秒数。
     */
    private int defaultIntervalSeconds = 1;

    /**
     * 登录接口每秒请求数。
     */
    private double loginCount = 20D;

    /**
     * 登录接口时间窗口秒数。
     */
    private int loginIntervalSeconds = 1;

    /**
     * AI 视频接口每秒请求数。
     */
    private double aiVideoCount = 10D;

    /**
     * AI 视频接口时间窗口秒数。
     */
    private int aiVideoIntervalSeconds = 1;

    /**
     * /api 接口每秒请求数。
     */
    private double apiCount = 40D;

    /**
     * /api 接口时间窗口秒数。
     */
    private int apiIntervalSeconds = 1;

    /**
     * chat-ws 每秒握手数。
     */
    private double chatWsCount = 30D;

    /**
     * chat-ws 时间窗口秒数。
     */
    private int chatWsIntervalSeconds = 1;

    /**
     * video 每秒握手数。
     */
    private double videoWsCount = 15D;

    /**
     * video 时间窗口秒数。
     */
    private int videoWsIntervalSeconds = 1;
}
