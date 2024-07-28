package com.daiyc.extension.core.impl;

import com.daiyc.extension.core.ExtensionLoader;
import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.meta.ExtensionPointInfo;
import com.daiyc.extension.util.NameGenerateUtils;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.ConstructorUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author daiyc
 * @since 2024/7/21
 */
@SuppressWarnings("unchecked")
public class ExtensionLoaderImpl<T> implements ExtensionLoader<T> {
    private final Class<T> type;

    private final ExtensionPointInfo extensionPointInfo;

    private final ExtensionRegistry<T> extensionRegistry;

    protected final AtomicReference<T> adaptiveExtension = new AtomicReference<>();

    public ExtensionLoaderImpl(Class<T> type, ExtensionRegistry<T> extensionRegistry) {
        this.type = type;
        this.extensionPointInfo = new ExtensionPointInfo(type);
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public T getExtension() {
        T e = adaptiveExtension.get();
        if (e != null) {
            return e;
        }
        return adaptiveExtension.updateAndGet(ext -> ext == null ? getAdaptiveExtension() : ext);
    }

    @SneakyThrows
    protected T getAdaptiveExtension() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> adaptiveClass = contextClassLoader.loadClass(NameGenerateUtils.generateAdaptiveFullClassName(type));
        return (T) ConstructorUtils.invokeConstructor(adaptiveClass,
                new Object[]{extensionRegistry}, new Class[]{ExtensionRegistry.class});
    }

    @SneakyThrows
    @Override
    public T getExtension(String name) {
        return extensionRegistry.get(name);
    }
}
