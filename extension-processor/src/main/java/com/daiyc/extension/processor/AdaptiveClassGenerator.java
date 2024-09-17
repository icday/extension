package com.daiyc.extension.processor;

import com.daiyc.extension.adaptive.matcher.PatternMatcher;
import com.daiyc.extension.adaptive.matcher.PatternMatchers;
import com.daiyc.extension.adaptive.matcher.TypeMatcher;
import com.daiyc.extension.adaptive.matcher.TypeMatchers;
import com.daiyc.extension.core.AdaptiveExtension;
import com.daiyc.extension.core.ExtensionNameConverter;
import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.ObjectFactory;
import com.daiyc.extension.core.annotations.Adaptive;
import com.daiyc.extension.core.annotations.ExtensionPoint;
import com.daiyc.extension.core.enums.DegradationStrategy;
import com.daiyc.extension.core.exceptions.MismatchExtensionException;
import com.daiyc.extension.processor.exception.TypeIncompatibleException;
import com.daiyc.extension.processor.meta.AdaptiveMeta;
import com.daiyc.extension.util.ExtensionNamingUtils;
import com.squareup.javapoet.*;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
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
@SuppressWarnings("unchecked")
public class AdaptiveClassGenerator {
    protected final ProcessingEnvironment processingEnv;

    protected final TypeElement interfaze;

    protected final Elements elementUtils;

    protected final Types typeUtils;

    protected final TypeElement objectTypeElement;

    protected final Map<Tuple2<String, String>, MethodSpec> helpMethods = new HashMap<>();

    protected final Scope classScope = new Scope();

    protected TypeSpec cache = null;

    protected final TypeSpec.Builder classBuilder;

    private DeclaredType enumType;

    private List<String> allowNames = Collections.emptyList();

    public AdaptiveClassGenerator(ProcessingEnvironment processingEnv, TypeElement interfaze) {
        this.processingEnv = processingEnv;
        this.interfaze = interfaze;
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.objectTypeElement = elementUtils.getTypeElement("java.lang.Object");

        classBuilder = TypeSpec.classBuilder(ExtensionNamingUtils.generateAdaptiveSimpleClassName(interfaze.getSimpleName().toString()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(interfaze.asType())
                .addSuperinterface(ClassName.get(AdaptiveExtension.class));
    }

    public TypeSpec generate() {
        if (cache != null) {
            return cache;
        }

        ParameterizedTypeName registryType = ParameterizedTypeName.get(ClassName.get(ExtensionRegistry.class), ClassName.get(interfaze));
        Map<String, AnnotationValue> extensionPointAnnValues = AnnotationUtils.getAnnotationValues(interfaze, ExtensionPoint.class);
        String defaultExtName = (String) extensionPointAnnValues.get("value").getValue();
        allowNames = ((List<AnnotationValue>) extensionPointAnnValues.get("allowNames").getValue())
                .stream()
                .map(v -> v.getValue().toString())
                .collect(Collectors.toList());

        this.enumType = AnnotationUtils.getEnumType(extensionPointAnnValues, "enumType");

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
        Scope scope = Scope.fromFunction(parameters);

        VariableElement adaptiveParam = getAdaptiveParam(method);

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
            DeclaredType enumType = ObjectUtils.defaultIfNull(toEnumMeta.getEnumType(), this.enumType);

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
            String varTypeMatchers = classScope.newVar("typeMatchers");
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(TypeMatchers.class, varTypeMatchers, Modifier.PROTECTED, Modifier.FINAL);
            List<AdaptiveMeta.ByTypeMeta> byTypes = adaptiveMeta.getByTypes();
            CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
            codeBlockBuilder.add("$T.as(", TypeMatchers.class);
            for (AdaptiveMeta.ByTypeMeta byType : byTypes) {
                codeBlockBuilder.add("$T.as($S", TypeMatcher.class, byType.getName());
                for (DeclaredType type : byType.getTypes()) {
                    codeBlockBuilder.add(", $T.class", type);
                }
                codeBlockBuilder.add(")");
            }
            codeBlockBuilder.add(")");
            fieldBuilder.initializer(codeBlockBuilder.build());
            classBuilder.addField(fieldBuilder.build());

            methodBuilder.addStatement("$T $L = $L.apply($L.findExt($L))", String.class, keyStrVarName, converterVarName, varTypeMatchers, keyVarName);
        } else if (CollectionUtils.isNotEmpty(adaptiveMeta.getByPatterns())) {
            String varPatternMatchers = classScope.newVar("patternMatchers");
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(PatternMatchers.class, varPatternMatchers, Modifier.PROTECTED, Modifier.FINAL);
            List<AdaptiveMeta.ByPatternMeta> byPatterns = adaptiveMeta.getByPatterns();
            CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
            codeBlockBuilder.add("$T.as(", PatternMatchers.class);
            for (AdaptiveMeta.ByPatternMeta byPattern : byPatterns) {
                codeBlockBuilder.add("$T.as($S", PatternMatcher.class, byPattern.getName());
                for (String pattern : byPattern.getPatterns()) {
                    codeBlockBuilder.add(", $S", pattern);
                }
                codeBlockBuilder.add(")");
            }
            codeBlockBuilder.add(")");
            fieldBuilder.initializer(codeBlockBuilder.build());
            classBuilder.addField(fieldBuilder.build());

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

    protected static VariableElement getAdaptiveParam(ExecutableElement method) {
        int idx = getAdaptiveParamIndex(method);
        return method.getParameters().get(idx);
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
