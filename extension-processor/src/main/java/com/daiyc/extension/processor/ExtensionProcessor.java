package com.daiyc.extension.processor;

import com.daiyc.extension.core.AdaptiveExtension;
import com.daiyc.extension.core.ExtensionNameConverter;
import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.ObjectFactory;
import com.daiyc.extension.core.annotations.Adaptive;
import com.daiyc.extension.util.NameGenerateUtils;
import com.daiyc.extension.util.PropertyRetrieveUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.SneakyThrows;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        TypeElement objectTypeElement = elementUtils.getTypeElement("java.lang.Object");
        String packageName = elementUtils.getPackageOf(interfaze).getQualifiedName().toString();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(NameGenerateUtils.generateAdaptiveSimpleClassName(interfaze.getSimpleName().toString()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(interfaze.asType())
                .addSuperinterface(ClassName.get(AdaptiveExtension.class));

        ParameterizedTypeName registryType = ParameterizedTypeName.get(ClassName.get(ExtensionRegistry.class), ClassName.get(interfaze));

        classBuilder.addField(
                registryType, "registry", Modifier.PROTECTED, Modifier.FINAL
        );

        classBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(registryType, "registry")
                        .addStatement("this.registry = registry")
                        .build()
        );

        // 这里不包含继承接口的方法
        ElementFilter.methodsIn(elementUtils.getAllMembers(interfaze))
                .stream()
                .filter(m -> !m.getEnclosingElement().equals(objectTypeElement))
                .filter(m -> !m.getModifiers().contains(Modifier.DEFAULT))
                .map(m -> this.generateMethodSpec(interfaze, m))
                .forEach(classBuilder::addMethod);

        return JavaFile.builder(packageName, classBuilder.build())
                .build();
    }

    protected static int getAdaptiveParamIndex(ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            if (parameter.getAnnotation(Adaptive.class) != null){
                return i;
            }
        }
        return -1;
    }

    private MethodSpec generateMethodSpec(TypeElement interfaze, ExecutableElement method) {
        if (getAdaptiveParamIndex(method) > -1) {
            return generateAdaptiveMethodSpec(interfaze, method);
        } else {
            return generateUnsupportedMethodSpec(interfaze, method);
        }
    }

    private MethodSpec generateAdaptiveMethodSpec(TypeElement interfaze, ExecutableElement method) {
        MethodSpec.Builder methodBuilder = newMethodBuilder(interfaze, method);

        List<? extends VariableElement> parameters = method.getParameters();

        int adaptiveParamIndex = getAdaptiveParamIndex(method);
        VariableElement adaptiveParam = parameters.get(adaptiveParamIndex);

        Map<String, AnnotationValue> annotationValues = AnnotationUtils.getAnnotationValues(adaptiveParam, Adaptive.class);
        TypeMirror converterType = (TypeMirror) annotationValues.get("converter").getValue();
        String path = annotationValues.get("value").getValue().toString();

        methodBuilder.addStatement("String key = $T.retrieveKey($L, $S)", PropertyRetrieveUtils.class, adaptiveParam.getSimpleName(), path);
        methodBuilder.addStatement("$T converter = $T.getInstance().get($T.class)", ExtensionNameConverter.class, ObjectFactory.class, converterType);
        methodBuilder.addStatement("key = converter.convert(key)");
        methodBuilder.addStatement("$L extension = registry.get(key)", interfaze.getSimpleName());
        String args = parameters.stream()
                .map(VariableElement::getSimpleName)
                .collect(Collectors.joining(", "));
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            methodBuilder.addStatement("return extension.$L($L)", method.getSimpleName(), args);
        } else {
            methodBuilder.addStatement("extension.$L($L)", method.getSimpleName(), args);
        }

        return methodBuilder.build();
    }

    private MethodSpec generateUnsupportedMethodSpec(TypeElement interfaze, ExecutableElement method) {
        MethodSpec.Builder methodBuilder = newMethodBuilder(interfaze, method);

        methodBuilder.addStatement("throw new UnsupportedOperationException()");

        return methodBuilder.build();
    }

    protected MethodSpec.Builder newMethodBuilder(TypeElement interfaze, ExecutableElement method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(method.getReturnType()));

        List<? extends VariableElement> parameters = method.getParameters();

        for (VariableElement parameter : parameters) {
            TypeMirror parameterType = parameter.asType();
            if (parameterType instanceof TypeVariable) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, parameter + " is TypeVariable");
                TypeMirror resolvedType = findRealType(interfaze, method, (TypeVariable) parameterType);
                methodBuilder.addParameter(ClassName.get(resolvedType), parameter.getSimpleName().toString());
            } else {
                methodBuilder.addParameter(ClassName.get(parameterType), parameter.getSimpleName().toString());
            }
        }
        return methodBuilder;
    }

    protected TypeMirror findRealType(TypeElement interfaze, ExecutableElement method, TypeVariable typeVariable) {
        if (interfaze.equals(method.getEnclosingElement())) {
            throw new IllegalArgumentException("ExtensionPoint interface MUST NOT have any type variables");
        }
        return doFindRealType(interfaze.asType(), method, typeVariable);
    }

    protected TypeMirror doFindRealType(TypeMirror superInterface, ExecutableElement method, TypeVariable typeVariable) {
        DeclaredType declaredType = (DeclaredType) superInterface;

        Element enclosingElement = method.getEnclosingElement();
        // 方法定义的接口
        if (declaredType.asElement().equals(enclosingElement)) {
            return processingEnv.getTypeUtils().asMemberOf(declaredType, typeVariable.asElement());
        }

        List<? extends TypeMirror> parentInterfaces = ((TypeElement) ((DeclaredType) superInterface).asElement()).getInterfaces();
        if (parentInterfaces.isEmpty()) {
            return null;
        }

        for (TypeMirror parentInterface : parentInterfaces) {
            TypeMirror typeMirror = doFindRealType(parentInterface, method, typeVariable);
            if (typeMirror != null) {
                return typeMirror;
            }
        }
        return null;
    }

    private boolean isTypeElement(Element element) {
        return element instanceof TypeElement;
    }
}
