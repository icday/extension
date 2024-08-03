package com.daiyc.extension.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import lombok.SneakyThrows;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * @author daiyc
 * @since 2024/7/23
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.daiyc.extension.core.annotations.ExtensionPoint")
public class ExtensionProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found @ExtensionPoint at " + element);
                processPoint(element);
            }
        }
        return true;
    }

    @SneakyThrows
    protected void processPoint(Element element) {
        if (!isTypeElement(element)) {
            return;
        }

        TypeElement typeElement = (TypeElement) element;

        JavaFile javaFile = generateClass(typeElement);
        javaFile.writeTo(processingEnv.getFiler());
    }

    protected JavaFile generateClass(TypeElement interfaze) {
        Elements elementUtils = processingEnv.getElementUtils();
        String packageName = elementUtils.getPackageOf(interfaze).getQualifiedName().toString();

        AdaptiveClassGenerator adaptiveClassGenerator = new AdaptiveClassGenerator(processingEnv, interfaze);

        return JavaFile.builder(packageName, adaptiveClassGenerator.generate())
                .build();
    }

    private boolean isTypeElement(Element element) {
        return element instanceof TypeElement;
    }
}
