package com.daiyc.extension.core;

import java.util.function.Supplier;

/**
 * @author daiyc
 * @since 2024/7/27
 */
public interface ExtensionRegistry<T> {
    <C extends T> void register(Class<C> type, Supplier<C> supplier);

    T get();

    T get(String name);
}
