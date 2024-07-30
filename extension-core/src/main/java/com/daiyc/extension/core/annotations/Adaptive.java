package com.daiyc.extension.core.annotations;

import com.daiyc.extension.core.ExtensionNameConverter;
import com.daiyc.extension.core.converter.DefaultNameConverter;

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

    /**
     * 当指定参数无法获取
     */
    boolean useDefault() default false;

    /**
     * 将参数转换成 extension name
     */
    Class<? extends ExtensionNameConverter> converter() default DefaultNameConverter.class;
}
