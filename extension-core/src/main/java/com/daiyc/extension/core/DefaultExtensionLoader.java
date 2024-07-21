package com.daiyc.extension.core;

import com.daiyc.extension.core.annotations.Extension;
import com.daiyc.extension.core.meta.ExtensionPointInfo;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author daiyc
 * @since 2024/7/21
 */
@SuppressWarnings("unchecked")
public class DefaultExtensionLoader<T> implements ExtensionLoader<T> {
    private final Class<T> type;

    private final ExtensionPointInfo extensionPointInfo;

    private final Map<String, Class<? extends T>> extensionClasses = new ConcurrentHashMap<>();

    protected final AtomicReference<T> adaptiveExtension = new AtomicReference<>();

    public DefaultExtensionLoader(Class<T> type) {
        this.type = type;
        this.extensionPointInfo = new ExtensionPointInfo(type);
    }

    @Override
    public T getExtension() {
        T e = adaptiveExtension.get();
        if (e != null) {
            return e;
        }
        return adaptiveExtension.updateAndGet(ext -> ext == null ? getAdaptiveExtension() : ext);
    }

    protected T getAdaptiveExtension() {
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            ExtensionPointInfo.MethodInfo methodInfo = extensionPointInfo.getMethodInfo(method);
            if (!methodInfo.isAdaptive()) {
                throw new IllegalArgumentException("method is not adaptive:" + method);
            }

            Integer idx = methodInfo.getParamIndex();
            String path = methodInfo.getPath();

            String key = retrieveKey(args[idx], path);

            T extension = getExtension(key);
            return method.invoke(extension, args);
        };

        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, invocationHandler);
    }

    @SneakyThrows
    protected String retrieveKey(Object param, String path) {
        if (path == null || path.isEmpty()) {
            return param.toString();
        }
        return BeanUtils.getProperty(param, path);
    }

    @SneakyThrows
    @Override
    public T getExtension(String name) {
        Class<? extends T> extensionClass = extensionClasses.get(name);
        return extensionClass.newInstance();
    }

    boolean register(Class<? extends T> clazz) {
        Extension ext = clazz.getAnnotation(Extension.class);
        if (ext != null) {
            extensionClasses.put(ext.value(), clazz);
            return true;
        }
        return false;
    }
}
