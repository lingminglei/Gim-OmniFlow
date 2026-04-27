package org.lml.config.sensitive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 日志脱敏注解
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sensitive {
    SensitiveDataType type() default SensitiveDataType.PASSWORD;
}
