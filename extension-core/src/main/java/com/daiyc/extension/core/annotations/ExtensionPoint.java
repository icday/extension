package com.daiyc.extension.core.annotations;

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
}
