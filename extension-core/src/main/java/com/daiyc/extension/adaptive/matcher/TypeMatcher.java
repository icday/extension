package com.daiyc.extension.adaptive.matcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@RequiredArgsConstructor
public class TypeMatcher implements Predicate<Object> {
    private final List<Class<?>> types;

    @Getter
    private final String name;

    @Override
    public boolean test(Object o) {
        return types.stream()
                .anyMatch(type -> type.isAssignableFrom(o.getClass()));
    }

    public static TypeMatcher as(String name, Class<?>... types) {
        return new TypeMatcher(Arrays.asList(types), name);
    }
}
