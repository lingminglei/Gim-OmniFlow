package org.lml.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import javax.annotation.Resource;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${ufop.local-storage-path}")
    public String filePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        /**
         * 访问的URL路径
         */
        registry.addResourceHandler("/upload/**")
                /**
                 * 静态资源文件的存储位置
                 */
//                .addResourceLocations("file:" + filePath);
                .addResourceLocations("file:" + filePath + "/upload/");
    }

}
