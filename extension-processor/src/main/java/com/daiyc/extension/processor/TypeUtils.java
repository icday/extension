package com.daiyc.extension.processor;

import com.squareup.javapoet.TypeName;

/**
 * @author daiyc
 * @since 2024/8/18
 */
public abstract class TypeUtils {
    public static TypeName box(TypeName type) {
        return type.isPrimitive() ? type.box() : type;
    }
}
