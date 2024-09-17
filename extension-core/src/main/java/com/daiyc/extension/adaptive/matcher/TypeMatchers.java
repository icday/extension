package com.daiyc.extension.adaptive.matcher;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@RequiredArgsConstructor
public class TypeMatchers {
    private final List<TypeMatcher> matchers;

    public String findExt(Object value) {
        return matchers.stream()
                .filter(m -> m.test(value))
                .map(TypeMatcher::getName)
                .findFirst()
                .orElse(null);
    }

    public static TypeMatchers as(TypeMatcher... matchers) {
        return new TypeMatchers(Arrays.asList(matchers));
    }
}
