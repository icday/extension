package com.daiyc.extension.adaptive.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author daiyc
 * @since 2024/12/14
 */
public class ChainMatcher<T, R, M extends Matcher<T, R>> implements Matcher<T, R> {
    private final List<M> matchers = new ArrayList<>();

    @SafeVarargs
    public static <T, R, M extends Matcher<T, R>> Matcher<T, R> as(M... matchers) {
        return new ChainMatcher<>(matchers);
    }

    @SafeVarargs
    public ChainMatcher(M... matchers) {
        this.matchers.addAll(Arrays.asList(matchers));
    }

    @Override
    public R match(T t) {
        return matchers.stream()
                .map(matcher -> matcher.match(t))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
