package com.daiyc.extension.processor.generator.method;

import com.daiyc.extension.adaptive.matcher.*;
import com.daiyc.extension.core.exceptions.MismatchExtensionException;
import com.daiyc.extension.processor.AnnotationUtils;
import com.daiyc.extension.processor.ElementUtils;
import com.daiyc.extension.processor.generator.AdaptiveClassGenerator;
import com.daiyc.extension.processor.generator.MatchType;
import com.daiyc.extension.processor.meta.AdaptiveMeta;
import com.squareup.javapoet.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.stream.Collectors;

import static com.daiyc.extension.processor.generator.MatchType.*;

/**
 * @author daiyc
 * @since 2024/12/11
 */
public class AdaptiveMethodGenerator extends BaseMethodGenerator {
    protected final VariableElement adaptiveParam;

    private final AdaptiveMeta adaptiveMeta;

    private final RetrieveMethodGenerator retrieveMethodGenerator;

    private FieldSpec matcherField;

    public AdaptiveMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method, int index
            , VariableElement adaptiveParam) {
        super(classGenerator, method, index);

        this.adaptiveParam = adaptiveParam;

        this.adaptiveMeta = AnnotationUtils.readAdaptive(adaptiveParam);

        adaptiveMeta.validateParamAnnotation();

        this.retrieveMethodGenerator = new RetrieveMethodGenerator(adaptiveParam.asType(), adaptiveMeta.getValue());
    }

    @Override
    public void preGenerate() {
        String retrieveMethodName = classGenerator.registerRetrieveMethod(retrieveMethodGenerator);
        retrieveMethodGenerator.setMethodName(retrieveMethodName);

        MatchType matchType = adaptiveMeta.getMatchType();
        FieldSpec.Builder builder;
        if (matchType == BY_TYPE) {
            builder = newMatcherFieldBuilder(Matcher.class, Object.class);
            CodeBlock.Builder code = CodeBlock.builder();
            code.add("$T.as(", ChainMatcher.class);
            boolean isFirstMatcher = true;
            for (AdaptiveMeta.ByTypeMeta byType : adaptiveMeta.getByTypes()) {
                if (!isFirstMatcher) {
                    code.add(",");
                }

                code.add("$T.as($S", TypeMatcher.class, byType.getName());
                byType.getTypes().forEach(type -> code.add(", $T.class", type));
                code.add(")");

                isFirstMatcher = false;
            }
            code.add(")");

            builder.initializer(code.build());
        } else if (matchType == BY_PATTERN) {
            builder = newMatcherFieldBuilder(Matcher.class, String.class);
            CodeBlock.Builder code = CodeBlock.builder();
            code.add("$T.as(", ChainMatcher.class);
            boolean isFirstMatcher = true;
            for (AdaptiveMeta.ByPatternMeta patternMeta: adaptiveMeta.getByPatterns()) {
                if (!isFirstMatcher) {
                    code.add(",");
                }

                code.add("$T.as($S", PatternMatcher.class, patternMeta.getName());
                patternMeta.getPatterns().forEach(pattern -> code.add(", $S", pattern));
                code.add(")");

                isFirstMatcher = false;
            }
            code.add(")");

            builder.initializer(code.build());
        } else if (matchType == TO_ENUM) {
            AdaptiveMeta.ToEnumMeta toEnumMeta = adaptiveMeta.getToEnumMeta();
            DeclaredType enumType = ObjectUtils.defaultIfNull(toEnumMeta.getEnumType(), classGenerator.getExtensionPointMeta().getEnumType());
            assert enumType != null;

            switch (toEnumMeta.getMatchType()) {
                case BY_METHOD:
                    ExecutableElement methodElement = ElementUtils.findMethod(enumType, toEnumMeta.getByMethod(), true);
                    assert methodElement.getParameters().size() == 1;
                    builder = newMatcherFieldBuilder(Matcher.class, methodElement.getParameters().get(0).asType());
                    builder.initializer("$T.byMethod($T.class, $T::$L)", EnumMatcher.class, enumType, enumType, methodElement.getSimpleName());
                    break;
                case BY_FIELD:
                    VariableElement field = ElementUtils.findField(enumType, toEnumMeta.getByField());
                    assert field != null;
                    builder = newMatcherFieldBuilder(Matcher.class, field.asType());
                    builder.initializer("$T.byField($T.class, $T::get$L)", EnumMatcher.class, enumType, enumType, StringUtils.capitalize(field.getSimpleName().toString()));
                    break;
                case BY_ORDINAL:
                    builder = newMatcherFieldBuilder(Matcher.class, Integer.class);
                    builder.initializer("$T.byOrdinal($T.class)", EnumMatcher.class, enumType);
                    break;
                default:
                    throw new IllegalStateException("Unsupported match type: " + toEnumMeta.getMatchType());
            }
        } else {
            builder = newMatcherFieldBuilder(Matcher.class, Object.class);
            builder.initializer("new $T()", ToStringMatcher.class);
        }
        matcherField = builder.build();
        classGenerator.addField(matcherField);
    }

    protected FieldSpec.Builder newMatcherFieldBuilder(Class<?> type, TypeName fromType) {
        return FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(type), fromType.box(), ClassName.get(String.class))
                , "matcher" + index, Modifier.PRIVATE);
    }

    protected FieldSpec.Builder newMatcherFieldBuilder(Class<?> type, TypeMirror fromType) {
        return newMatcherFieldBuilder(type, TypeName.get(fromType));
    }

    protected FieldSpec.Builder newMatcherFieldBuilder(Class<?> type, Class<?> fromType) {
        return newMatcherFieldBuilder(type, ClassName.get(fromType));
    }

    @Override
    public MethodSpec generate() {
        MethodSpec.Builder methodBuilder = newMethodBuilder(interfaze, method);

        TypeName keyPropertyType = ClassName.get(retrieveMethodGenerator.getReturnType());

        // 需要定义的局部变量
        String keyVarName = scope.newVar("key");
        String keyStrVarName = scope.newVar("keyStr");
        String extensionVarName = scope.newVar("extension");

        // 读取指定路径的参数
        methodBuilder.addStatement("$T $L = $L($L)", keyPropertyType, keyVarName, retrieveMethodGenerator.getMethodName(), adaptiveParam.getSimpleName());

        methodBuilder.addStatement("$T $L = $L.match($L)", String.class, keyStrVarName, matcherField.name, keyVarName);
        methodBuilder.addStatement("$L = getExtensionName($L, $L, $S)", keyStrVarName, keyStrVarName, adaptiveMeta.isUseDefault(), adaptiveMeta.getDefaultExtension());

        methodBuilder.beginControlFlow("if ($L == null)", keyStrVarName);
        methodBuilder.addStatement("throw new $T($T.class)", MismatchExtensionException.class, interfaze);
        methodBuilder.endControlFlow();

        methodBuilder.addStatement("$L $L = this.getExtension($L)", interfaze.getSimpleName(), extensionVarName, keyStrVarName);

        methodBuilder.beginControlFlow("if ($L == null)", extensionVarName);
        methodBuilder.addStatement("throw new $T($T.class, $L)", MismatchExtensionException.class, interfaze, keyStrVarName);
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
}
