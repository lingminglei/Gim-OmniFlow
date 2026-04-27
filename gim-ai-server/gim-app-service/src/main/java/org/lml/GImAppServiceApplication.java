package org.lml;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
//import org.lml.kafka.temp.ProducerService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("org.lml.mapper")
@EnableScheduling
@EnableDubbo
@EnableDiscoveryClient
@Slf4j(topic = "lml")
//@EnableKafka
public class GImAppServiceApplication {

	public static void main(String[] args) {

		log.info("======================[ GIm-Chat ] start  ================");
		SpringApplication.run(GImAppServiceApplication.class, args);

		log.info("======================[ GIm-Chat ] 服务启动完成！ ================");
	}

}
