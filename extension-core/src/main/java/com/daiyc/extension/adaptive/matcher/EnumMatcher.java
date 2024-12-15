package com.daiyc.extension.adaptive.matcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/12/14
 */
@Getter
@RequiredArgsConstructor
public abstract class EnumMatcher<T, E extends Enum<E>> implements Matcher<T, String> {
    protected final Class<E> enumType;

    public static <T extends Enum<T>> EnumMatcher<Integer, T> byOrdinal(Class<T> enumType) {
        return new EnumOrdinalMatcher<>(enumType);
    }

    public static <T extends Enum<T>, F> EnumMatcher<F, T> byField(Class<T> enumType, Function<T, F> getter) {
        return new EnumFieldMatcher<>(enumType, getter);
    }

    public static <T extends Enum<T>, V, F extends Function<V, T>> EnumMatcher<V, T> byMethod(Class<T> enumType, F fn) {
        return new EnumMethodMatcher<>(enumType, fn);
    }

    @Override
    public String match(T t) {
        E e = doMatch(t);
        if (e == null) {
            return null;
        }
        return e.name();
    }

    protected abstract E doMatch(T t);

    public static class EnumOrdinalMatcher<T extends Enum<T>> extends EnumMatcher<Integer, T> {
        public EnumOrdinalMatcher(Class<T> enumType) {
            super(enumType);
        }

        @Override
        public T doMatch(Integer ordinal) {
            if (ordinal == null || ordinal < 0 || ordinal >= enumType.getEnumConstants().length) {
                return null;
            }
            return enumType.getEnumConstants()[ordinal];
        }
    }

    public static class EnumFieldMatcher<T extends Enum<T>, F> extends EnumMatcher<F, T> {
        private final Map<F, T> map;

        public EnumFieldMatcher(Class<T> enumType, Function<T, F> getter) {
            super(enumType);

            map = Arrays.stream(enumType.getEnumConstants())
                    .collect(Collectors.toMap(getter, Function.identity()));
        }

        @Override
        public T doMatch(F f) {
            return map.get(f);
        }
    }

    public static class EnumMethodMatcher<T extends Enum<T>, V, F extends Function<V, T>> extends EnumMatcher<V, T> {
        private final F function;

        public EnumMethodMatcher(Class<T> enumType, F fn) {
            super(enumType);

            this.function = fn;
        }

        @Override
        public T doMatch(V v) {
            return function.apply(v);
        }
    }
}
