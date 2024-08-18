package com.daiyc.extension.core.annotations;

import com.daiyc.extension.core.ExtensionNameConverter;
import com.daiyc.extension.core.converter.DefaultNameConverter;
import com.daiyc.extension.core.enums.DegradationStrategy;

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
     * 指定的参数是 int 类型时，可以转换成一个枚举值，用于匹配对应的扩展名称。<br/>
     * 只能设置一个值
     */
    ToEnum[] toEnum() default {};

    /**
     * 将参数转换成 extension name
     */
    Class<? extends ExtensionNameConverter> converter() default DefaultNameConverter.class;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface ToEnum {
        /**
         * 枚举类
         */
        Class<? extends Enum<?>> enumClass();

        /**
         * 通过静态方法映射枚举值，指定对应方法名
         */
        String byMethod() default "";

        /**
         * 通过字段找到对应的枚举值
         */
        String byField() default "";

        /**
         * 通过 ordinal 映射成枚举值
         */
        boolean byOrdinal() default false;
    }
}
