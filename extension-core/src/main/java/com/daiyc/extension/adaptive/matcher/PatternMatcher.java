package com.daiyc.extension.adaptive.matcher;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@RequiredArgsConstructor
public class PatternMatcher implements Matcher<String, String> {
    private final List<Pattern> patterns;

    private final String name;

    @Override
    public String match(String s) {
        boolean b = patterns.stream()
                .anyMatch(p -> p.matcher(s).matches());
        return b ? name : null;
    }

    public static PatternMatcher as(String name, String... patterns) {
        return new PatternMatcher(Arrays.stream(patterns).map(Pattern::compile).collect(Collectors.toList()), name);
    }
}
