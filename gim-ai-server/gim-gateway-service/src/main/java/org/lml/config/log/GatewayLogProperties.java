package org.lml.config.log;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关日志配置。
 */
@Getter
@Component
@ConfigurationProperties(prefix = "gateway.log")
public class GatewayLogProperties {

    /**
     * 请求体日志最大长度。
     */
    private int bodyMaxLength = 2048;

    /**
     * 设置请求体日志最大长度。
     *
     * @param bodyMaxLength 最大长度
     */
    public void setBodyMaxLength(int bodyMaxLength) {
        this.bodyMaxLength = bodyMaxLength;
    }
}
