package com.daiyc.extension.core.impl;

import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.annotations.Extension;
import com.daiyc.extension.core.exceptions.DuplicateExtensionNameException;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author daiyc
 * @since 2024/7/27
 */
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class ExtensionRegistryImpl<T> implements ExtensionRegistry<T> {
    protected final Class<T> type;

    protected Map<String, T> cache = new ConcurrentHashMap<>();

    protected Map<String, Supplier<? extends T>> factories = new ConcurrentHashMap<>();

    @Override
    public <C extends T> void register(Class<C> type, Supplier<C> supplier) {
        Extension ann = type.getAnnotation(Extension.class);
        String[] names = ann.value();
        for (String name : names) {
            if (factories.containsKey(name)) {
                throw new DuplicateExtensionNameException(type, "Extension name conflict: %s!", name);
            }

            factories.put(name, supplier);
        }
    }

    @Override
    public T get(String name) {
        return cache.computeIfAbsent(name, n -> factories.get(name).get());
    }
}
