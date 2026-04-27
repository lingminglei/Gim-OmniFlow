package org.lml.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = "frameExecutor")
    public Executor frameExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：根据你的并发用户数调整
        executor.setCorePoolSize(5); 
        // 最大线程数
        executor.setMaxPoolSize(10); 
        // 队列容量：缓冲等待处理的帧
        executor.setQueueCapacity(100); 
        // 线程前缀
        executor.setThreadNamePrefix("FrameProcess-");
        
        // 拒绝策略：当堆积太多时，丢弃最旧的任务，防止 OOM
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        executor.initialize();
        return executor;
    }
}
