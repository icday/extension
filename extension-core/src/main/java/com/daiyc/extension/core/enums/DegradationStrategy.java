package com.daiyc.extension.core.enums;

/**
 * 未找到指定扩展时的降级策略
 *
 * @author daiyc
 * @since 2024/8/3
 */
public enum DegradationStrategy {
    /**
     * 不做任何兜底
     */
    NONE,

    /**
     * 当指定的 key 为 null 时，使用默认扩展
     */
    DEFAULT_IF_NULL,

    /**
     * 当指定的 key 为 null 或不匹配时，使用默认扩展
     */
    DEFAULT_IF_MISMATCH,

    ;
}
