package com.daiyc.extension.core;

import java.util.function.Supplier;

/**
 * @author daiyc
 * @since  2024/7/20
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface ExtensionPointRegistry {
    <T> void register(Class<T> clazz);

    <T> void register(Class<T> clazz, T ext);

    <T> void register(Class<T> clazz, Supplier<T> supplier);
}
