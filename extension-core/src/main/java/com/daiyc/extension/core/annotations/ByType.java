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
public @interface ByType {
    /**
     * 类型，满足其一即可
     */
    Class<?>[] type();

    /**
     * 扩展名<br/>需要遵守 {@link ExtensionPoint} 的 enumType 或 allowNames 的限制
     */
    String name();
}
