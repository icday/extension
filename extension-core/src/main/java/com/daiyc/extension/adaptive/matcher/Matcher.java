package com.daiyc.extension.adaptive.matcher;

/**
 * @author daiyc
 * @since 2024/12/14
 */
public interface Matcher<T, R> {

    R match(T t);
}
