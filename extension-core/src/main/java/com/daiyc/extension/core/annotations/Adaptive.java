package com.daiyc.extension.core.annotations;

import com.daiyc.extension.core.ExtensionNameConverter;
import com.daiyc.extension.core.converter.DefaultNameConverter;
import com.daiyc.extension.core.enums.DegradationStrategy;
import com.daiyc.extension.core.enums.EnumSearchType;

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
     * 当找不到匹配扩展时的降级策略
     */
    DegradationStrategy degradationStrategy() default DegradationStrategy.NONE;

    /**
     * 指定的参数是 int 类型时，可以转换成一个枚举值，用于匹配对应的扩展名称。
     */
    IntToEnum intToEnum();

    /**
     * 将参数转换成 extension name
     */
    Class<? extends ExtensionNameConverter> converter() default DefaultNameConverter.class;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface IntToEnum {
        /**
         * 枚举类
         */
        Class<? extends Enum<?>> value();

        /**
         * 转换方式
         */
        EnumSearchType type() default EnumSearchType.ORDINAL;

        /**
         * 具体的转换方法或者字段
         */
        String name() default "";
    }
}
