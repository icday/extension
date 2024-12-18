package com.daiyc.extension.adaptive.matcher;

/**
 * @author daiyc
 * @since 2024/12/18
 */
public class ToStringMatcher implements Matcher<Object, String> {
    @Override
    public String match(Object o) {
        if (o == null) {
            return null;
        }

        return o.toString();
    }
}
