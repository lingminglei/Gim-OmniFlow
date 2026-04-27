package org.lml.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        try {
            // 测试连接
            redissonClient.getKeys().count();
            log.info("✅ [Redisson] 测试连接成功 at {}", LocalDateTime.now().format(DATE_FORMATTER));
            return new RedissonConnectionFactory(redissonClient);
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            log.error("❌ [Redisson] 连接失败 failed at {} | Error: {}", timestamp, e.getMessage());
            throw new IllegalStateException("Redisson connection failed at " + timestamp, e);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedissonConnectionFactory redissonConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        try {
            // 配置序列化
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

            // 设置连接工厂
            template.setConnectionFactory(redissonConnectionFactory);
            template.afterPropertiesSet();

            log.info("✅ [RedisTemplate] 初始化成功 successfully at {}", LocalDateTime.now().format(DATE_FORMATTER));
            return template;
        } catch (Exception e) {
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            log.error("❌ [RedisTemplate] 初始化失败 at {} | Error: {}", timestamp, e.getMessage());
            throw new BeanCreationException("RedisTemplate initialization failed at " + timestamp, e);
        }
    }
}
