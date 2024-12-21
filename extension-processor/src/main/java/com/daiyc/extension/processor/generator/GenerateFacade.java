package com.daiyc.extension.processor.generator;

import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * @author daiyc
 * @since 2024/12/19
 */
public class GenerateFacade {
    private final ProcessingEnvironment processingEnv;

    private final GenerateContext generateContext;

    public GenerateFacade(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;

        this.generateContext = new GenerateContext(processingEnv);
    }

    public TypeSpec generate(TypeElement interfaze) {
        try {
            AdaptiveClassGenerator adaptiveClassGenerator = new AdaptiveClassGenerator(generateContext, interfaze);
            adaptiveClassGenerator.preGenerate();
            return adaptiveClassGenerator.generate();
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate class for " + interfaze + ": " + ex);
            throw ex;
        }
    }
}
