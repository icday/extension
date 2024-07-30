package com.daiyc.extension.core.impl;

import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.annotations.Extension;
import com.daiyc.extension.util.NameGenerateUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.ConstructorUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author daiyc
 * @since 2024/7/27
 */
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class ExtensionRegistryImpl<T> implements ExtensionRegistry<T> {
    protected final Class<T> type;

    protected final AtomicReference<T> adaptive = new AtomicReference<>();

    protected Map<String, T> cache = new ConcurrentHashMap<>();

    protected Map<String, Supplier<? extends T>> factories = new ConcurrentHashMap<>();

    @Override
    public <C extends T> void register(Class<C> type, Supplier<C> supplier) {
        Extension ann = type.getAnnotation(Extension.class);
        if (factories.containsKey(ann.value())) {
            throw new IllegalStateException("Duplicate extension name: " + ann.value());
        }

        factories.put(ann.value(), supplier);
    }

    @Override
    public T get() {
        T ext = adaptive.get();
        if (ext != null) {
            return null;
        }

        return adaptive.updateAndGet(prev -> prev != null ? prev : createAdaptive());
    }

    @SneakyThrows
    protected T createAdaptive() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        String adaptiveClassName = NameGenerateUtils.generateAdaptiveFullClassName(type);
        Class<?> adaptiveClass = contextClassLoader.loadClass(adaptiveClassName);

        return (T) ConstructorUtils.invokeConstructor(adaptiveClass,
                new Object[]{this}, new Class[]{ExtensionRegistry.class});
    }

    @Override
    public T get(String name) {
        return cache.computeIfAbsent(name, n -> factories.get(name).get());
    }
}
