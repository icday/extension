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
     * 扩展实现的唯一名称
     */
    String[] value();
}
