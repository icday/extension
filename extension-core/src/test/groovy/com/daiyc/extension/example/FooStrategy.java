package com.daiyc.extension.example;

import com.daiyc.extension.core.annotations.Extension;

/**
 * @author daiyc
 * @since 2024/7/20
 */
@Extension("foo")
public class FooStrategy implements Strategy {
    @Override
    public String getType(String type) {
        return "foo";
    }

    @Override
    public String show(Arguments args) {
        return String.format("foo: %s", args.getNumber());
    }
}
