package com.daiyc.extension.core.converter;

import com.daiyc.extension.core.ExtensionNameConverter;

/**
 * @author daiyc
 * @since 2024/7/29
 */
public class DefaultNameConverter implements ExtensionNameConverter {
    @Override
    public String convert(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Enum) {
            return ((Enum) value).name();
        }

        return value.toString();
    }
}
