package com.daiyc.extension.processor.generator.method;

import com.daiyc.extension.processor.generator.AdaptiveClassGenerator;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * @author daiyc
 * @since 2024/12/15
 */
public class UnsupportedMethodGenerator extends BaseMethodGenerator {
    public UnsupportedMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method, int index) {
        super(classGenerator, method, index);
    }

    @Override
    public MethodSpec generate() {
        return generateUnsupportedMethodSpec(classGenerator.getInterfaze(), method);
    }
    private MethodSpec generateUnsupportedMethodSpec(TypeElement interfaze, ExecutableElement method) {
        MethodSpec.Builder methodBuilder = newMethodBuilder(interfaze, method);

        methodBuilder.addStatement("throw new UnsupportedOperationException()");

        return methodBuilder.build();
    }
}
