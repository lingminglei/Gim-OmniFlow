package org.lml.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 网关独立服务启动类。
 */
@SpringBootApplication
@Slf4j
public class GimAiGateWayServiceApplication {

    public static void main(String[] args) {
        log.info("业务 网关服务开始启动");
        SpringApplication.run(GimAiGateWayServiceApplication.class, args);
        log.info("业务 网关服务启动完成");
    }
}
