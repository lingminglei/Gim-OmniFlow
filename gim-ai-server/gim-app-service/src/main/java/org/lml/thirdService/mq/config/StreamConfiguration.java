package org.lml.thirdService.mq.config;

import org.lml.thirdService.mq.producer.StreamProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Hollis
 */
@Configuration
public class StreamConfiguration {
    @Bean
    public StreamProducer streamProducer() {
        StreamProducer streamProducer = new StreamProducer();
        return streamProducer;
    }
}
