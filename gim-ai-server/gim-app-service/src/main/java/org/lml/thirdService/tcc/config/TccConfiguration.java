package org.lml.thirdService.tcc.config;

import org.lml.thirdService.tcc.service.TransactionLogService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Hollis
 */
@Configuration
@MapperScan("org.lml.thirdService.tcc.mapper")
public class TccConfiguration {

    @Bean
    public TransactionLogService transactionLogService() {
        return new TransactionLogService();
    }
}
