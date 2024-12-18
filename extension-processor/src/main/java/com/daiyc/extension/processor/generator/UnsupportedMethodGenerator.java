package com.daiyc.extension.processor.generator;

import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * @author daiyc
 * @since 2024/12/15
 */
public class UnsupportedMethodGenerator extends BaseMethodGenerator {
    private final AdaptiveClassGenerator classGenerator;

    private final ExecutableElement method;

    public UnsupportedMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method) {
        this.classGenerator = classGenerator;
        this.processingEnv = classGenerator.processingEnv;
        this.method = method;
    }

    @Override
    public boolean preGenerate(GenerateContext ctx) {
        return true;
    }

    @Override
    public MethodSpec generate() {
        return generateUnsupportedMethodSpec(classGenerator.interfaze, method);
    }
    private MethodSpec generateUnsupportedMethodSpec(TypeElement interfaze, ExecutableElement method) {
        MethodSpec.Builder methodBuilder = newMethodBuilder(interfaze, method);

        methodBuilder.addStatement("throw new UnsupportedOperationException()");

        return methodBuilder.build();
    }
}
