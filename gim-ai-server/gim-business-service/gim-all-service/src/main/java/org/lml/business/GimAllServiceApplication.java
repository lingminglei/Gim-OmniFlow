package org.lml.business;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 业务聚合服务启动类。
 * 统一扫描 billing 与 AI 模块，让被引入的业务模块随 all-service 一起启动并暴露 Dubbo 服务。
 */
@EnableDubbo
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "org.lml.business",
        "org.lml.AiGateWay",
        "org.lml.AiService"
})
public class GimAllServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GimAllServiceApplication.class, args);
    }
}
