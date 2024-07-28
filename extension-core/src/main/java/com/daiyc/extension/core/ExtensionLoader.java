package com.daiyc.extension.core;

/**
 * @author daiyc
 * @since  2024/7/16
 */
public interface ExtensionLoader<T> {
    /**
     * 获取指定类型的自适应扩展
     *
     * @return 自适应的扩展对象
     */
    T getExtension();

    /**
     * 获取对应的扩展实例
     * @param name 名称
     */
    T getExtension(String name);
}
