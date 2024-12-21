package com.daiyc.extension.processor.generator;

import com.squareup.javapoet.MethodSpec;

/**
 * @author daiyc
 * @since 2024/12/15
 */
public interface MethodGenerator {
    void preGenerate();

    MethodSpec generate();
}
