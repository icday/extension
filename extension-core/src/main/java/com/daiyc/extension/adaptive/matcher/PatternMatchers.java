package com.daiyc.extension.adaptive.matcher;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@RequiredArgsConstructor
public class PatternMatchers {
    private final List<PatternMatcher> matchers;

    public String findExt(String value) {
        return matchers.stream()
                .filter(m -> m.test(value))
                .map(PatternMatcher::getName)
                .findFirst()
                .orElse(null);
    }

    public static PatternMatchers as(PatternMatcher... matchers) {
        return new PatternMatchers(Arrays.asList(matchers));
    }
}
