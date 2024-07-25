package com.daiyc.extension.processor;

import com.daiyc.extension.core.annotations.Adaptive;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import lombok.SneakyThrows;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
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

    protected JavaFile generateClass(TypeElement element) {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(element.getSimpleName() + "Impl0")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(element.asType());

        processingEnv.getElementUtils().getAllMembers(element)
                .stream()
                .filter(m -> m.getKind() == ElementKind.METHOD)
                .map(m -> (ExecutableElement) m)
                .filter(m -> !m.getModifiers().contains(Modifier.DEFAULT))
                .filter(m -> m.getParameters().stream().map(v -> (VariableElement) v)
                        .anyMatch(p -> p.getAnnotation(Adaptive.class) != null)
                )
                .map(this::generateMethodSpec)
                .forEach(classBuilder::addMethod);

        return JavaFile.builder(packageName, classBuilder.build())
                .build();
    }


    private MethodSpec generateMethodSpec(ExecutableElement method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(method.getReturnType()));

        for (VariableElement parameter : method.getParameters()) {
            methodBuilder.addParameter(ClassName.get(parameter.asType()), parameter.getSimpleName().toString());
        }

        // Add a simple return statement for non-void methods
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            methodBuilder.addStatement("return null");
        }

        return methodBuilder.build();
    }

    private MethodSpec generateUnsupportedMethodSpec(ExecutableElement method) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(method.getReturnType()));

        for (VariableElement parameter : method.getParameters()) {
            methodBuilder.addParameter(ClassName.get(parameter.asType()), parameter.getSimpleName().toString());
        }

        // Add a simple return statement for non-void methods
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            methodBuilder.addStatement("throw new UnsupportedOperationException()");
        }

        return methodBuilder.build();
    }

    private boolean isTypeElement(Element element) {
        return element instanceof TypeElement;
    }
}
