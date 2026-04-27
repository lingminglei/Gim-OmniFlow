package org.lml.AiService.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * UiUiAPI聚合平台
 * 【配置文件】
 */
@Data
@Component
@ConfigurationProperties(prefix = "uiuiapi")
public class UiUiConfig {

    private String apiKey;
    private String baseUrl;
}
