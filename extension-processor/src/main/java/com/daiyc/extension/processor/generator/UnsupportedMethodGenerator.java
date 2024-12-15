package com.daiyc.extension.processor.generator;

import javax.lang.model.element.ExecutableElement;

/**
 * @author daiyc
 * @since 2024/12/15
 */
public class UnsupportedMethodGenerator extends BaseMethodGenerator {
    public UnsupportedMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method) {
        super(classGenerator, method);
    }
}
