package com.daiyc.extension.processor.generator;

import com.squareup.javapoet.TypeSpec;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * @author daiyc
 * @since 2024/12/19
 */
@RequiredArgsConstructor
public class GenerateFacade {
    private final ProcessingEnvironment processingEnv;

    public TypeSpec generate(TypeElement interfaze) {
        try {
            AdaptiveClassGenerator adaptiveClassGenerator = new AdaptiveClassGenerator(processingEnv, interfaze);
            adaptiveClassGenerator.preGenerate(new GenerateContext());
            return adaptiveClassGenerator.generate();
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate class for " + interfaze + ": " + ex);
            throw ex;
        }
    }
}
