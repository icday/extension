package com.daiyc.extension.core;

import com.daiyc.extension.adaptive.matcher.EnumMatcher.EnumFieldMatcher;
import com.daiyc.extension.adaptive.matcher.EnumMatcher.EnumMethodMatcher;
import com.daiyc.extension.adaptive.matcher.EnumMatcher.EnumOrdinalMatcher;
import com.daiyc.extension.adaptive.matcher.Matcher;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Function;

/**
 * @author daiyc
 * @since 2024/12/8
 */

@RequiredArgsConstructor
public abstract class BaseAdaptiveExtension<EXT> implements AdaptiveExtension {
    protected final ExtensionRegistry<EXT> registry;

    /**
     * 候选扩展名称，为空表示不限制
     */
    protected final Set<String> candidateExtensionNames;

    /**
     * 默认扩展名，可为空
     */
    protected final String defaultExtensionName;

    protected final List<Matcher<Object, String>> typeMatchers = new ArrayList<>();

    protected final List<Matcher<String, String>> patternMatchers = new ArrayList<>();

    protected final Map<Class<?>, EnumOrdinalMatcher<?>>
            enumOrdinalMatcherMap = new HashMap<>();

    protected final Map<Tuple2<Class<?>, Function<?, ?>>, EnumFieldMatcher<?, ?>>
            enumFieldMatcherMap = new HashMap<>();

    protected final Map<Tuple2<Class<?>, Function<?, ?>>, EnumMethodMatcher<?, ?, ?>>
            enumMethodMatcherMap = new HashMap<>();

    public BaseAdaptiveExtension(ExtensionRegistry<EXT> registry) {
        this(registry, null, null);
    }

    public BaseAdaptiveExtension(ExtensionRegistry<EXT> registry, Set<String> candidateExtensionNames) {
        this(registry, candidateExtensionNames, null);
    }

    protected void addTypeMatcher(Matcher<Object, String> matcher) {
        typeMatchers.add(matcher);
        patternMatchers.add(null);
    }

    protected void addPatternMatcher(Matcher<String, String> matcher) {
        typeMatchers.add(null);
        patternMatchers.add(matcher);
    }

    protected Matcher<Object, String> typeMatcher(int index) {
        return typeMatchers.get(index);
    }

    protected Matcher<String, String> patternMatcher(int index) {
        return patternMatchers.get(index);
    }

    protected <E extends Enum<E>> EnumOrdinalMatcher<E> enumOrdinalMatcher(Class<E> enumType) {
        return (EnumOrdinalMatcher<E>) enumOrdinalMatcherMap.get(enumType);
    }

    protected <E extends Enum<E>, F> EnumFieldMatcher<E, F> enumFieldMatcher(Class<E> enumType, Class<F> fieldType, Function<E, F> getter) {
        return (EnumFieldMatcher<E, F>) enumFieldMatcherMap.get(Tuple.of(enumType, getter));
    }

    protected <E extends Enum<E>, V, FUNC extends Function<V, E>> EnumMethodMatcher<E, V, FUNC> enumMethodMatcher(Class<E> enumType, FUNC function) {
        return (EnumMethodMatcher<E, V, FUNC>) enumMethodMatcherMap.get(Tuple.of(enumType, function));
    }
}
