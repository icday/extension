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
@SupportedAnnotationTypes({
        ExtensionConstants.EXTENSION_POINT,
        ExtensionConstants.EXTENSION,
})
public class ExtensionProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            String annName = annotation.getQualifiedName().toString();
            boolean ok = true;
            if (annName.equals(ExtensionConstants.EXTENSION_POINT)) {
                ok = handleExtensionPoint(annotation, roundEnv);
            } else if (annName.equals(ExtensionConstants.EXTENSION)) {
                ok = handleExtension(annotation, roundEnv);
            }

            if (!ok) {
                return false;
            }
        }
        return true;
    }

    protected boolean handleExtensionPoint(TypeElement annotation, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (!isTypeElement(element)) {
                continue;
            }

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found @ExtensionPoint at " + element);
            if (!processPoint(element)) {
                return false;
            }
        }
        return true;
    }

    protected boolean handleExtension(TypeElement annotation, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            if (!isTypeElement(element)) {
                continue;
            }

            if (!checkExtension(element, roundEnv)) {
                return false;
            }
        }
        return true;
    }

    protected boolean checkExtension(Element element, RoundEnvironment roundEnv) {
        return true;
    }

    @SneakyThrows
    protected boolean processPoint(Element element) {
        if (!isTypeElement(element)) {
            return true;
        }

        TypeElement typeElement = (TypeElement) element;

        JavaFile javaFile = generateClass(typeElement);
        javaFile.writeTo(processingEnv.getFiler());
        return true;
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
