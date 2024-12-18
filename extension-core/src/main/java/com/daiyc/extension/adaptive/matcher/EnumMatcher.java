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

    @Override
    public String match(T t) {
        E e = getEnum(t);
        return e == null ? null : e.name();
    }

    protected abstract E getEnum(T t);

    public static <E extends Enum<E>> EnumMatcher<Integer, E> byOrdinal(Class<E> enumType) {
        return new EnumOrdinalMatcher<>(enumType);
    }

    public static <E extends Enum<E>, T> EnumMatcher<T, E> byField(Class<E> enumType, Function<E, T> getter) {
        return new EnumFieldMatcher<>(enumType, getter);
    }

    public static <E extends Enum<E>, T> EnumMatcher<T, E> byMethod(Class<E> enumType, Function<T, E> fn) {
        return new EnumMethodMatcher<>(enumType, fn);
    }

    public static class EnumOrdinalMatcher<E extends Enum<E>> extends EnumMatcher<Integer, E> {
        public EnumOrdinalMatcher(Class<E> enumType) {
            super(enumType);
        }

        @Override
        public E getEnum(Integer ordinal) {
            if (ordinal == null || ordinal < 0 || ordinal >= enumType.getEnumConstants().length) {
                return null;
            }
            return enumType.getEnumConstants()[ordinal];
        }
    }

    public static class EnumFieldMatcher<E extends Enum<E>, T> extends EnumMatcher<T, E> {
        private final Map<T, E> map;

        public EnumFieldMatcher(Class<E> enumType, Function<E, T> getter) {
            super(enumType);

            map = Arrays.stream(enumType.getEnumConstants())
                    .collect(Collectors.toMap(getter, Function.identity()));
        }

        @Override
        protected E getEnum(T t) {
            return map.get(t);
        }
    }

    public static class EnumMethodMatcher<E extends Enum<E>, V> extends EnumMatcher<V, E> {
        private final Function<V, E> function;

        public EnumMethodMatcher(Class<E> enumType, Function<V, E> function) {
            super(enumType);
            this.function = function;
        }

        @Override
        protected E getEnum(V v) {
            return function.apply(v);
        }
    }
}
