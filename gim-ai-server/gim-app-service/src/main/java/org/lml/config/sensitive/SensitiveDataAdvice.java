package org.lml.config.sensitive;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lml.common.result.CommonResult;
import org.lml.utils.DesensitizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;

/**
 * 做 敏感信息脱敏 拦截器
 */
@ControllerAdvice
public class SensitiveDataAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final static Logger log = LoggerFactory.getLogger(SensitiveDataAdvice.class);

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 拦截所有响应
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, org.springframework.http.MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  org.springframework.http.server.ServerHttpRequest request,
                                  org.springframework.http.server.ServerHttpResponse response) {
//        log.info("进入自定义敏感信息脱敏拦截器");
        // 如果返回类型是result
        if (body instanceof CommonResult<?>){
            // 处理对象，进行脱敏操作
            handleSensitiveFields((CommonResult<?>) body);
        }

        return body;
    }

    private void handleSensitiveFields(CommonResult<?> res) {
        Object data = res.getData();

        //获取data的下的全部字段
        if (data == null) {
            return;
        }
        Field[] fields = data.getClass().getDeclaredFields();
        for (Field field : fields) {
            // 判断是否有 @SensitiveData 注解
            if (field.isAnnotationPresent(Sensitive.class)) {
                log.info("@SensitiveData注解处理字段: {}", field.getName());
                Sensitive annotation = field.getAnnotation(Sensitive.class);
                SensitiveDataType sensitiveDataType = annotation.type();
                field.setAccessible(true);
                try {
                    Object value = field.get(data);
                    if (value instanceof String) {
                        // 执行脱敏操作
                        log.info("执行脱敏操作！");
                        String maskedValue = DesensitizationUtils.maskData((String) value, sensitiveDataType.getType());
                        log.info("脱敏前数据值：{}，脱敏后数据值：{}",(String) value,maskedValue);
                        field.set(data, maskedValue);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
