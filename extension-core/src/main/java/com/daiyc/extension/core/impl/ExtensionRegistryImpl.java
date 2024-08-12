package com.daiyc.extension.core.impl;

import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.annotations.Extension;
import com.daiyc.extension.core.annotations.ExtensionPoint;
import com.daiyc.extension.core.enums.None;
import com.daiyc.extension.core.exceptions.DuplicateExtensionNameException;
import com.daiyc.extension.core.exceptions.MismatchExtensionException;
import com.daiyc.extension.util.ExtensionNamingUtils;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author daiyc
 * @since 2024/7/27
 */
@SuppressWarnings("unchecked")
public class ExtensionRegistryImpl<T> implements ExtensionRegistry<T> {
    protected final Class<T> type;

    protected final boolean unifyName;

    protected final Set<String> availableNames;

    protected Map<String, T> cache = new ConcurrentHashMap<>();

    protected Map<String, Supplier<? extends T>> factories = new ConcurrentHashMap<>();

    public ExtensionRegistryImpl(Class<T> type) {
        this.type = type;
        ExtensionPoint ann = type.getAnnotation(ExtensionPoint.class);
        unifyName = ann.unifyName();

        this.availableNames = getAvailableNames(ann);
    }

    protected Set<String> getAvailableNames(ExtensionPoint ann) {
        Class<? extends Enum<?>> enumClass = ann.enumerable();
        if (enumClass != null && !enumClass.equals(None.class)) {
            return Try.of(() -> {
                Method m = enumClass.getMethod("values");
                return Stream.of((Enum[]) m.invoke(enumClass))
                        .map(Enum::name)
                        .map(this::format)
                        .collect(Collectors.toSet());
            }).getOrElse(Collections.emptySet());
        }

        return Stream.of(ann.names())
                .map(this::format)
                .collect(Collectors.toSet());
    }

    protected String format(String name) {
        if (!unifyName || StringUtils.isBlank(name)) {
            return name;
        }
        return ExtensionNamingUtils.unifyExtensionName(name);
    }

    @Override
    public <C extends T> void register(Class<C> type, Supplier<C> supplier) {
        Extension ann = type.getAnnotation(Extension.class);
        String[] names = ann.value();
        for (String name : names) {
            name = format(name);
            if (factories.containsKey(name)) {
                throw new DuplicateExtensionNameException(type, "Extension name conflict: %s! at: %s", name, type.getName());
            }

            factories.put(name, supplier);
        }
    }

    @Override
    public T get(String name) {
        String extName = format(name);
        return cache.computeIfAbsent(extName, n -> Optional.ofNullable(factories.get(n))
                .map(Supplier::get)
                .orElseThrow(() -> new MismatchExtensionException(type, "name: %s", name)));
    }
}
