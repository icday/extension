package com.daiyc.extension.adaptive.matcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@RequiredArgsConstructor
public class TypeMatcher implements Matcher<Object, String> {
    private final List<Class<?>> types;

    @Getter
    private final String name;

    @Override
    public String match(Object o) {
        return types.stream()
                .filter(type -> type.isAssignableFrom(o.getClass()))
                .findFirst()
                .map(t -> name)
                .orElse(null);
    }

    public static TypeMatcher as(String name, Class<?>... types) {
        return new TypeMatcher(Arrays.asList(types), name);
    }
}
