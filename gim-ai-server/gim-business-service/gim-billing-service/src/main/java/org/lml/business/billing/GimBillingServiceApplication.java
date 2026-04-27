package org.lml.business.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 积分账务服务启动类。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "org.lml.business.billing")
public class GimBillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GimBillingServiceApplication.class, args);
    }
}
