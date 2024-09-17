package com.daiyc.extension.core.annotations;

import com.daiyc.extension.core.enums.None;

import java.lang.annotation.*;

/**
 * 扩展点
 *
 * @author daiyc
 * @since  2024/7/20
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ExtensionPoint {
    /**
     * 默认扩展实现
     */
    String value() default "";

    /**
     * 限制扩展实现名称只能与枚举名称保持一致
     */
    Class<? extends Enum<?>> enumType() default None.class;

    /**
     * 如果未定义枚举，也想限制扩展实现名称只能为以下值
     */
    String[] allowNames() default {};

    /**
     * 将驼峰、下划线形式的名称都统一化
     */
    boolean unifyName() default true;
}
