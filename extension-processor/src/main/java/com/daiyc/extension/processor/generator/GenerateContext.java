package com.daiyc.extension.processor.generator;

import lombok.Getter;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * @author daiyc
 * @since 2024/12/16
 */
@Getter
public class GenerateContext {
    private final ProcessingEnvironment processingEnv;

    private final Elements elementUtils;

    private final Types typeUtils;

    private final TypeElement objectTypeElement;

    public GenerateContext(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.objectTypeElement = elementUtils.getTypeElement(Object.class.getCanonicalName());
    }

    public Messager getMessager() {
        return processingEnv.getMessager();
    }
}
