package com.daiyc.extension.processor;

import com.daiyc.extension.core.AdaptiveExtension;
import com.daiyc.extension.core.ExtensionNameConverter;
import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.ObjectFactory;
import com.daiyc.extension.core.annotations.Adaptive;
import com.daiyc.extension.core.annotations.ExtensionPoint;
import com.daiyc.extension.core.enums.DegradationStrategy;
import com.daiyc.extension.core.exceptions.MismatchExtensionException;
import com.daiyc.extension.processor.exception.TypeIncompatibleException;
import com.daiyc.extension.util.ExtensionNamingUtils;
import com.squareup.javapoet.*;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/7/31
 */
public class AdaptiveClassGenerator {
    protected final ProcessingEnvironment processingEnv;

    protected final TypeElement interfaze;

    protected final Elements elementUtils;

    protected final Types typeUtils;

    protected final TypeElement objectTypeElement;

    protected final Map<Tuple2<String, String>, MethodSpec> helpMethods = new HashMap<>();

    protected TypeSpec cache = null;

    public AdaptiveClassGenerator(ProcessingEnvironment processingEnv, TypeElement interfaze) {
        this.processingEnv = processingEnv;
        this.interfaze = interfaze;
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.objectTypeElement = elementUtils.getTypeElement("java.lang.Object");
    }

    public TypeSpec generate() {
        if (cache != null) {
            return cache;
        }

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(ExtensionNamingUtils.generateAdaptiveSimpleClassName(interfaze.getSimpleName().toString()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(interfaze.asType())
                .addSuperinterface(ClassName.get(AdaptiveExtension.class));

        ParameterizedTypeName registryType = ParameterizedTypeName.get(ClassName.get(ExtensionRegistry.class), ClassName.get(interfaze));
        String defaultExtName = (String) AnnotationUtils.getAnnotationValues(interfaze, ExtensionPoint.class).get("value").getValue();

        classBuilder
                .addField(registryType, "registry", Modifier.PROTECTED, Modifier.FINAL)
                .addField(String.class, "defaultExtName", Modifier.PROTECTED, Modifier.FINAL);

        classBuilder.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(registryType, "registry")
                        .addStatement("this.registry = registry")
                        .addStatement("this.defaultExtName = $S", defaultExtName)
                        .build()
        );

        Stream.ofAll(getAllInterfaceMethods())
                .map(m -> this.generateMethodSpec(interfaze, m))
                .forEach(classBuilder::addMethod);

        helpMethods.values()
                .forEach(classBuilder::addMethod);

        return cache = classBuilder.build();
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

        Set<String> scope = parameters.stream()
                .map(VariableElement::getSimpleName)
                .map(Object::toString)
                .collect(Collectors.toSet());

        int adaptiveParamIndex = getAdaptiveParamIndex(method);
        VariableElement adaptiveParam = parameters.get(adaptiveParamIndex);

        Map<String, AnnotationValue> annotationValues = AnnotationUtils.getAnnotationValues(adaptiveParam, Adaptive.class);
        TypeMirror converterType = (TypeMirror) annotationValues.get("converter").getValue();
        String path = annotationValues.get("value").getValue().toString();
        String degradationStrategyName = annotationValues.get("degradationStrategy").getValue().toString();
        DegradationStrategy degradationStrategy = DegradationStrategy.valueOf(degradationStrategyName);

        // retrieve method
        TypeMirror paramType = adaptiveParam.asType();

        Tuple2<String, String> retrieveMethodKey = Tuple.of("retrieveKey", ClassName.get(paramType).toString());
        MethodSpec retrieveMethod = helpMethods.computeIfAbsent(retrieveMethodKey, k -> generateRetrieveMethod(k._1, paramType, path));
        TypeName keyPropertyType = retrieveMethod.returnType;

        // 需要定义的局部变量
        String keyVarName = newVariableName(scope, "key");
        String keyStrVarName = newVariableName(scope, "keyStr");
        String extensionVarName = newVariableName(scope, "extension");
        String converterVarName = newVariableName(scope, "converter");

        // get key string
        methodBuilder.addStatement("$T $L = $L($L)", keyPropertyType, keyVarName, retrieveMethod.name, adaptiveParam.getSimpleName());

        List<AnnotationMirror> toEnums = (List<AnnotationMirror>) annotationValues.get("toEnum").getValue();
        if (toEnums.size() > 1) {
            throw new IllegalArgumentException("@Adaptive(toEnum) can ONLY contains ONE element");
        }
        if (!toEnums.isEmpty()) {
            Map<String, AnnotationValue> toEnumAnnValues = AnnotationUtils.getAnnotationValues(toEnums.get(0));
            DeclaredType enumType = (DeclaredType) toEnumAnnValues.get("enumClass").getValue();

            String byMethod = toEnumAnnValues.get("byMethod").getValue().toString();
            String byField = toEnumAnnValues.get("byField").getValue().toString();
            boolean byOrdinal = (boolean) toEnumAnnValues.get("byOrdinal").getValue();

            int flag = 0;
            if (StringUtils.isNotBlank(byMethod)) {
                flag ++;
            }
            if (StringUtils.isNotBlank(byField)) {
                flag ++;
            }
            if (byOrdinal) {
                flag ++;
            }

            if (flag == 0) {
                throw new IllegalArgumentException("@ToEnum MUST specify any of byMethod, byField or byOrdinal strategy");
            }

            if (flag > 1) {
                throw new IllegalArgumentException("@ToEnum MUST ONLY specify one of byMethod, byField or byOrdinal strategy");
            }

            String keyEnumVarName = "keyEnum";

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
                methodBuilder.addStatement("$T $L = $T.of($T.values()).filter(e -> Objects.equals(e.$L(), $L)).findFirst().get()",
                        enumType, keyEnumVarName, java.util.stream.Stream.class, enumType, getterMethod.getSimpleName(), keyVarName);
            }
            keyVarName = keyEnumVarName;
        }

        // get converter
        methodBuilder.addStatement("$T $L = $T.getInstance().get($T.class)", ExtensionNameConverter.class, converterVarName, ObjectFactory.class, converterType);
        // do convert
        methodBuilder.addStatement("String $L = $L.apply($L)", keyStrVarName, converterVarName, keyVarName);
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

    protected String newVariableName(Set<String> scope, String varName) {
        int i = 0;
        while (scope.contains(varName)) {
            varName += (i++);
        }

        scope.add(varName);

        return varName;
    }

    /**
     * 生成获取 key 值的辅助方法
     *
     * @param methodName 辅助方法名
     * @param paramType  参数类型
     * @param path       参数路径
     */
    private MethodSpec generateRetrieveMethod(String methodName, TypeMirror paramType, String path) {
        List<String> propertyNames = Arrays.asList(StringUtils.split(path, "."));
        TypeMirror returnType = ElementUtils.getDestType(paramType, propertyNames);

        String baseArg = "arg";
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get(paramType), baseArg)
                .returns(TypeName.get(returnType));

        Stream.ofAll(propertyNames)
                .foldLeft(Tuple.of(0, baseArg, paramType), (cur, propName) -> cur.apply((i, varName, varType) -> {
                    assert varType.getKind() == TypeKind.DECLARED;
                    Tuple2<VariableElement, ExecutableElement> property = ElementUtils.findProperty((DeclaredType) varType, propName);
                    builder.beginControlFlow("if ($L == null)", varName);
                    if (returnType.getKind().isPrimitive()) {
                        builder.addStatement("throw new $T()", NullPointerException.class);
                    } else {
                        builder.addStatement("return null");
                    }
                    builder.endControlFlow();

                    return property.apply((var, getter) -> {
                        String nextArgName = baseArg + i;
                        builder.addStatement("$T $L = $L.$L()", var.asType(), nextArgName, varName, getter.getSimpleName().toString());
                        return Tuple.of(i + 1, nextArgName, var.asType());
                    });
                })).apply((i, curArg, type) -> {
                    builder.addStatement("return " + curArg);
                    return null;
                });

        return builder.build();
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

    protected TypeMirror findRealType(TypeElement interfaze, ExecutableElement method, TypeVariable typeVariable) {
        if (interfaze.equals(method.getEnclosingElement())) {
            throw new IllegalArgumentException("ExtensionPoint interface MUST NOT have any type variables");
        }
        return doFindRealType(interfaze.asType(), method, typeVariable);
    }

    /**
     * 获取所有需要实现的方法
     */
    protected List<ExecutableElement> getAllInterfaceMethods() {
        return ElementFilter.methodsIn(elementUtils.getAllMembers(interfaze))
                .stream()
                .filter(m -> !m.getEnclosingElement().equals(objectTypeElement))
                .filter(m -> !m.getModifiers().contains(Modifier.DEFAULT))
                .collect(Collectors.toList());
    }

    protected static int getAdaptiveParamIndex(ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement parameter = parameters.get(i);
            if (parameter.getAnnotation(Adaptive.class) != null) {
                return i;
            }
        }
        return -1;
    }
}
