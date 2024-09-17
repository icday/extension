package com.daiyc.extension.adaptive.matcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@RequiredArgsConstructor
public class PatternMatcher implements Predicate<String> {
    private final List<Pattern> patterns;

    @Getter
    private final String name;

    @Override
    public boolean test(String s) {
        return patterns.stream().anyMatch(p -> p.matcher(s).matches());
    }

    public static PatternMatcher as(String name, String... patterns) {
        return new PatternMatcher(Arrays.stream(patterns).map(Pattern::compile).collect(Collectors.toList()), name);
    }
}
