package com.daiyc.extension.core;

import com.daiyc.extension.core.annotations.Extension;
import com.daiyc.extension.core.annotations.ExtensionPoint;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since  2024/7/20
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ExtensionRegistry {
    /**
     * ExtensionPoint -> ExtensionLoader
     */
    protected final Map<Class<?>, ExtensionLoader<?>> registry = new ConcurrentHashMap<>();

    public <T> void register(Class<T> clazz) {
        assert !clazz.isInterface();
        assert !Modifier.isAbstract(clazz.getModifiers());

        Extension ext = clazz.getAnnotation(Extension.class);
        if (ext == null) {
            return;
        }

        List<Class<?>> points = new ArrayList<>();

        Class<?> p = clazz;
        while (!p.equals(Object.class)) {
            List<Class<?>> eps = Arrays.stream(p.getInterfaces())
                    .filter(type -> type.isAnnotationPresent(ExtensionPoint.class))
                    .collect(Collectors.toList());
            points.addAll(eps);
            p = p.getSuperclass();
        }

        for (Class<?> point : points) {
            DefaultExtensionLoader<?> extensionLoader = (DefaultExtensionLoader<?>) registry.computeIfAbsent(point, DefaultExtensionLoader::new);
            extensionLoader.register((Class) clazz);
        }
    }

    public <T> ExtensionLoader<T> getExtensionLoader(Class<T> clazz) {
        return (ExtensionLoader<T>) registry.get(clazz);
    }

    public <T> boolean register(Class<T> clazz, T instance) {
        // todo
        return false;
    }

    public <T> T getExtension(Class<T> clazz) {
        return getExtension(clazz, null);
    }

    public <T> T getExtension(Class<T> clazz, ExtensionMatcher matcher) {
        return null;
    }
}
