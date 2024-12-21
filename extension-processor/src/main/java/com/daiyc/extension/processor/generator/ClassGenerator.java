package com.daiyc.extension.processor.generator;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * @author daiyc
 * @since 2024/12/16
 */
public interface ClassGenerator {
    TypeName getTypeName();

    void preGenerate();

    TypeSpec generate();
}
