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
    String[] candidates() default {};

    /**
     * 严格模式要求扩展实现名称必须与枚举名称保持一致<br>
     * 否则会尝试匹配
     */
    boolean strictMode() default false;

    /**
     * 没有指定扩展实现名称时是否使用默认
     */
    boolean useDefault() default false;

    /**
     * 默认扩展名
     */
    String defaultExtension() default "";
}
