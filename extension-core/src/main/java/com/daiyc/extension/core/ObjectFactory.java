package com.daiyc.extension.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author daiyc
 * @since 2024/7/29
 */
@SuppressWarnings("unchecked")
public class ObjectFactory {
    protected static final ObjectFactory INSTANCE = new ObjectFactory();

    protected Map<Class<?>, Object> cache = new ConcurrentHashMap<>();

    public static ObjectFactory getInstance() {
        return INSTANCE;
    }

    public <T> T get(Class<T> type) {
        return (T) cache.computeIfAbsent(type, t -> {
            try {
                return t.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
