package com.daiyc.extension.example;

import com.daiyc.extension.core.annotations.Extension;

/**
 * @author daiyc
 * @since 2024/7/20
 */
@Extension("bar")
public class BarStrategy implements Strategy {
    @Override
    public String getType(String type) {
        return "bar";
    }

    @Override
    public String show(Arguments args) {
        return String.format("bar: %s", args.getNumber());
    }
}
