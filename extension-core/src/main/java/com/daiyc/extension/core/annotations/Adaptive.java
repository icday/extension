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
     * 用于指定用于决策实现的参数名<br/>
     * 默认为参数值本身
     */
    String value() default "";

    /**
     * 当找不到匹配扩展时的降级策略
     */
    DegradationStrategy degradationStrategy() default DegradationStrategy.NONE;

    /**
     * 指定的参数是 int 类型时，可以转换成一个枚举值，用于匹配对应的扩展名称。<br/>
     * 只能指定一个
     */
    ToEnum[] toEnum() default {};

    /**
     * 根据参数的类型分派扩展实现<br/>
     * 可以指定多个，按照顺序匹配
     */
    ByType[] byType() default {};

    /**
     * 按照字符串匹配的正则来决定扩展。<br/>
     * 可以指定多个，按照顺序匹配
     */
    ByPattern[] byPattern() default {};

    /**
     * 将参数转换成 extension name
     */
    Class<? extends ExtensionNameConverter> converter() default DefaultNameConverter.class;
}
