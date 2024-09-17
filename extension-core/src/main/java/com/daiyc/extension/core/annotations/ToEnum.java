package com.daiyc.extension.core.annotations;

import com.daiyc.extension.core.enums.None;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ToEnum {
    /**
     * 目标枚举类<br/>
     * 默认为 {@link ExtensionPoint#enumType()} 指定的枚举类
     */
    Class<? extends Enum<?>> enumType() default None.class;

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
