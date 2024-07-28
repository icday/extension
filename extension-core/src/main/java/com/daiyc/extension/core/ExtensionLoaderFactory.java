package com.daiyc.extension.core;

/**
 * @author daiyc
 * @since 2024/7/27
 */
public interface ExtensionLoaderFactory {
    <T> ExtensionLoader<T> getExtensionLoader(Class<T> clazz);
}
