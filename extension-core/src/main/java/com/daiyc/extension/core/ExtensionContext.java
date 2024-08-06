package com.daiyc.extension.core;

import com.daiyc.extension.core.annotations.ExtensionPoint;
import com.daiyc.extension.core.impl.ExtensionLoaderImpl;
import com.daiyc.extension.core.impl.ExtensionRegistryImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/7/27
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ExtensionContext implements ExtensionLoaderFactory, ExtensionPointRegistry {
    protected final Map<Class<?>, ExtensionLoader<?>> loaders = new ConcurrentHashMap<>();

    protected final Map<Class<?>, ExtensionRegistry<?>> registries = new ConcurrentHashMap<>();

    @Override
    public <I> ExtensionLoader<I> getExtensionLoader(Class<I> clazz) {
        ExtensionRegistry<?> extensionRegistry = registries.computeIfAbsent(clazz, ExtensionRegistryImpl::new);
        return (ExtensionLoader<I>) loaders.computeIfAbsent(clazz, t ->
                new ExtensionLoaderImpl<>(clazz, (ExtensionRegistry<I>) extensionRegistry));
    }

    @Override
    public <I> void register(Class<I> clazz) {
        register(clazz, (Supplier<I>) () -> {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public <I> void register(Class<I> clazz, I ext) {
        register(clazz, (Supplier<I>) () -> ext);
    }

    @Override
    public <I> void register(Class<I> clazz, Supplier<I> supplier) {
        List<Class<?>> points = parseExtensionPoints(clazz);
        for (Class point : points) {
            doRegister(point, clazz, supplier);
        }
    }

    private <C> List<Class<?>> parseExtensionPoints(Class<C> clazz) {
        List<Class<?>> points = new ArrayList<>();

        Class<?> p = clazz;
        while (!p.equals(Object.class)) {
            List<Class<?>> eps = Arrays.stream(p.getInterfaces())
                    .filter(type -> type.isAnnotationPresent(ExtensionPoint.class))
                    .collect(Collectors.toList());
            points.addAll(eps);
            p = p.getSuperclass();
        }
        return points;
    }

    protected <I, C extends I> void doRegister(Class<I> interfaze, Class<C> clazz, Supplier<C> supplier) {
        ExtensionRegistry<?> extensionRegistry = registries.computeIfAbsent(interfaze, ExtensionRegistryImpl::new);
        extensionRegistry.register((Class) clazz, (Supplier) supplier);
    }
}
