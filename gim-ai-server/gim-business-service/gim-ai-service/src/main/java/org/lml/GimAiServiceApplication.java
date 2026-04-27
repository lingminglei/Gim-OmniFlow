package org.lml;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 网关独立服务启动类。
 */
@SpringBootApplication
@EnableDubbo
@Slf4j
public class GimAiServiceApplication {

    public static void main(String[] args) {
        log.info("AI服务网关开始启动");
        SpringApplication.run(GimAiServiceApplication.class, args);
        log.info("AI服务网关启动完成");
    }
}
