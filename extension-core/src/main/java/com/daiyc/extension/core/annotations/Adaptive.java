package com.daiyc.extension.core.annotations;

import java.lang.annotation.*;

/**
 * @author daiyc
 * @since  2024/7/20
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Adaptive {
    /**
     * 用于指定用于决策实现的参数名
     */
    String value() default "";
}
