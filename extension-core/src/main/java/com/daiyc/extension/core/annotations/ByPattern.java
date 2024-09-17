package com.daiyc.extension.core.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ByPattern {
    /**
     * 匹配的正则模式
     */
    String[] pattern();

    /**
     * 扩展名
     */
    String name();
}
