package com.daiyc.extension.processor.generator;

import com.daiyc.extension.core.ExtensionNameConverter;
import com.daiyc.extension.core.ObjectFactory;
import com.daiyc.extension.core.enums.DegradationStrategy;
import com.daiyc.extension.core.exceptions.MismatchExtensionException;
import com.daiyc.extension.processor.AnnotationUtils;
import com.daiyc.extension.processor.ElementUtils;
import com.daiyc.extension.processor.Scope;
import com.daiyc.extension.processor.TypeUtils;
import com.daiyc.extension.processor.exception.TypeIncompatibleException;
import com.daiyc.extension.processor.meta.AdaptiveMeta;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/12/11
 */
public class AdaptiveMethodGenerator extends BaseMethodGenerator {
    protected final AdaptiveClassGenerator classGenerator;

    protected final ExecutableElement method;

    protected final VariableElement adaptiveParam;

    final ProcessingEnvironment processingEnv;

    final TypeElement interfaze;

    public AdaptiveMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method, VariableElement adaptiveParam) {
        this.classGenerator = classGenerator;
        this.method = method;
        this.adaptiveParam = adaptiveParam;

        this.processingEnv = classGenerator.processingEnv;
        this.interfaze = classGenerator.interfaze;
    }

    @Override
    public boolean preGenerate(GenerateContext ctx) {
        AdaptiveMeta adaptiveMeta = AnnotationUtils.readAdaptive(adaptiveParam);
        return false;
    }

    @Override
    public MethodSpec generate() {
        MethodSpec.Builder methodBuilder = newMethodBuilder(interfaze, method);
        List<? extends VariableElement> parameters = method.getParameters();
        Scope scope = Scope.fromFunction(parameters);

        AdaptiveMeta adaptiveMeta = AnnotationUtils.readAdaptive(adaptiveParam);
        adaptiveMeta.validate();

        String path = adaptiveMeta.getValue();
        DegradationStrategy degradationStrategy = adaptiveMeta.getDegradationStrategy();

        // retrieve method
        TypeMirror paramType = adaptiveParam.asType();

        Tuple2<String, String> retrieveMethodKey = Tuple.of("retrieveKey", ClassName.get(paramType).toString());
        MethodSpec retrieveMethod = helpMethods.computeIfAbsent(retrieveMethodKey, k -> generateRetrieveMethod(k._1, paramType, path));
        TypeName keyPropertyType = retrieveMethod.returnType;

        // 需要定义的局部变量
        String keyVarName = scope.newVar("key");
        String keyStrVarName = scope.newVar("keyStr");
        String extensionVarName = scope.newVar("extension");
        String converterVarName = scope.newVar("converter");

        // 读取指定路径的参数
        methodBuilder.addStatement("$T $L = $L($L)", keyPropertyType, keyVarName, retrieveMethod.name, adaptiveParam.getSimpleName());
        methodBuilder.addStatement("$T $L = $T.getInstance().get($T.class)", ExtensionNameConverter.class, converterVarName, ObjectFactory.class, adaptiveMeta.getConverter());

        if (CollectionUtils.isNotEmpty(adaptiveMeta.getToEnums())) {
            AdaptiveMeta.ToEnumMeta toEnumMeta = adaptiveMeta.getToEnums().get(0);
            DeclaredType enumType = ObjectUtils.defaultIfNull(toEnumMeta.getEnumType(), this.extensionPointMeta.getEnumType());

            String byMethod = toEnumMeta.getByMethod();
            String byField = toEnumMeta.getByField();
            boolean byOrdinal = toEnumMeta.isByOrdinal();

            String keyEnumVarName = scope.newVar("keyEnum");

            if (StringUtils.isNotBlank(byMethod)) {
                methodBuilder.addStatement("$T $L = $T.$L($L)", enumType, keyEnumVarName, enumType, byMethod, keyVarName);
            } else if (byOrdinal) {
                if (!TypeUtils.box(keyPropertyType).equals(TypeName.INT.box())) {
                    throw new TypeIncompatibleException("Adaptive to enum value MUST BE int or integer type", interfaze);
                }

                methodBuilder.addStatement("$T $L = $T.of($T.values()).filter(e -> $T.equals(e.ordinal(), $L)).findFirst().get()",
                        enumType, keyEnumVarName, java.util.stream.Stream.class, enumType, Objects.class, keyVarName);
            } else if (StringUtils.isNotBlank(byField)) {
                ExecutableElement getterMethod = ElementUtils.findProperty(enumType, byField)._2();
                methodBuilder.addStatement("$T $L = $T.of($T.values()).filter(e -> $T.equals(e.$L(), $L)).findFirst().get()",
                        enumType, keyEnumVarName, java.util.stream.Stream.class, enumType, Objects.class, getterMethod.getSimpleName(), keyVarName);
            }
            methodBuilder.addStatement("$T $L = $L.apply($L)", String.class, keyStrVarName, converterVarName, keyEnumVarName);
        } else if (CollectionUtils.isNotEmpty(adaptiveMeta.getByTypes())) {
            String varTypeMatchers = registerTypeMatchersField(adaptiveMeta);

            methodBuilder.addStatement("$T $L = $L.apply($L.findExt($L))", String.class, keyStrVarName, converterVarName, varTypeMatchers, keyVarName);
        } else if (CollectionUtils.isNotEmpty(adaptiveMeta.getByPatterns())) {
            String varPatternMatchers = registerPatternMatchersField(adaptiveMeta);

            methodBuilder.addStatement("$T $L = $L.apply($L.findExt($L))", String.class, keyStrVarName, converterVarName, varPatternMatchers, keyVarName);
        } else {
            methodBuilder.addStatement("$T $L = $L.apply($L)", String.class, keyStrVarName, converterVarName, keyVarName);
        }

        // get converter
        // do convert
        if (degradationStrategy == DegradationStrategy.DEFAULT_IF_MISMATCH || degradationStrategy == DegradationStrategy.DEFAULT_IF_NULL) {
            methodBuilder.beginControlFlow("if ($L == null)", keyStrVarName);
            methodBuilder.addStatement("$L = this.defaultExtName", keyStrVarName);
            methodBuilder.endControlFlow();
        } else {
            methodBuilder.beginControlFlow("if ($L == null)", keyStrVarName);
            methodBuilder.addStatement("throw new $T($T.class)", MismatchExtensionException.class, interfaze);
            methodBuilder.endControlFlow();
        }

        methodBuilder.addStatement("$L $L = this.registry.get($L)", interfaze.getSimpleName(), extensionVarName, keyStrVarName);

        if (degradationStrategy == DegradationStrategy.DEFAULT_IF_MISMATCH) {
            methodBuilder.beginControlFlow("if ($L == null && $L != this.defaultExtName)", extensionVarName, keyStrVarName);
            methodBuilder.addStatement("$L = this.registry.get(this.defaultExtName)", extensionVarName);
            methodBuilder.endControlFlow();
        }

        methodBuilder.beginControlFlow("if ($L == null)", extensionVarName);
        methodBuilder.addStatement("throw new $T($T.class)", MismatchExtensionException.class, interfaze);
        methodBuilder.endControlFlow();

        String args = parameters.stream()
                .map(VariableElement::getSimpleName)
                .collect(Collectors.joining(", "));
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            methodBuilder.addStatement("return $L.$L($L)", extensionVarName, method.getSimpleName(), args);
        } else {
            methodBuilder.addStatement("$L.$L($L)", extensionVarName, method.getSimpleName(), args);
        }

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
}
