package com.daiyc.extension.core.annotations;

import java.lang.annotation.*;

/**
 * 扩展点实现
 *
 * @author daiyc
 * @since 2024/7/20
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Extension {
    /**
     * 扩展点处理匹配信息
     */
    String value();

    /**
     * 优先级
     */
    int priority() default 0;
}
