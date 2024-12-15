package com.daiyc.extension.processor.generator;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

/**
 * @author daiyc
 * @since 2024/12/11
 */
public class AdaptiveMethodGenerator extends BaseMethodGenerator {
    private final VariableElement adaptiveParam;

    public AdaptiveMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method, VariableElement adaptiveParam) {
        super(classGenerator, method);

        this.adaptiveParam = adaptiveParam;
    }

}
