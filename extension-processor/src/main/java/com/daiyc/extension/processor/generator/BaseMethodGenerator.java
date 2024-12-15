package com.daiyc.extension.processor.generator;

import lombok.RequiredArgsConstructor;

import javax.lang.model.element.ExecutableElement;

/**
 * @author daiyc
 * @since 2024/12/15
 */
@RequiredArgsConstructor
public abstract class BaseMethodGenerator implements MethodGenerator{
    protected final AdaptiveClassGenerator classGenerator;

    protected final ExecutableElement method;
}
