package org.lml.config.limit.configuration;

import org.lml.config.limit.SlidingWindowRateLimiter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流组件配置
 */
@Configuration
public class RateLimiterConfiguration {

    private final static Logger log = LoggerFactory
            .getLogger(RateLimiterConfiguration.class);

    @Bean
    public SlidingWindowRateLimiter slidingWindowRateLimiter(RedissonClient redisson) {

        log.info("ReatLimiter 限流组件初始化完成！");

        return new SlidingWindowRateLimiter(redisson);
    }
}
