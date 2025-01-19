package com.daiyc.extension.processor.generator.method;

import com.daiyc.extension.core.exceptions.MismatchExtensionException;
import com.daiyc.extension.processor.AnnotationUtils;
import com.daiyc.extension.processor.generator.AdaptiveClassGenerator;
import com.daiyc.extension.processor.meta.AdaptiveMeta;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/12/21
 */
public class SPELMethodGenerator extends BaseMethodGenerator {
    protected final AdaptiveMeta adaptiveMeta;

    private static final String PARSER_FIELD_NAME = "spelExpressionParser";

    private FieldSpec expressionField;

    public SPELMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method, int index) {
        super(classGenerator, method, index);

        adaptiveMeta = AnnotationUtils.readAdaptive(method);
    }

    @Override
    public void preGenerate() {
        if (!classGenerator.hasField(PARSER_FIELD_NAME)) {
            FieldSpec.Builder builder = FieldSpec.builder(SpelExpressionParser.class, PARSER_FIELD_NAME, Modifier.PRIVATE)
                    .initializer("new $T()", SpelExpressionParser.class);
            classGenerator.addField(builder.build());
        }

        FieldSpec.Builder builder = FieldSpec.builder(
                Expression.class,
                "spelExpression" + index,
                Modifier.PRIVATE
        );
        builder.initializer("$L.parseExpression($S)", PARSER_FIELD_NAME, adaptiveMeta.getValue());
        this.expressionField = builder.build();

        classGenerator.addField(this.expressionField);
    }

    @Override
    public MethodSpec generate() {
        MethodSpec.Builder methodBuilder = newMethodBuilder(classGenerator.getInterfaze(), method);

        String keyStrVarName = scope.newVar("key");
        String extensionVarName = scope.newVar("extension");
        String evaluationCtxVarName = scope.newVar("evaluationCtx");

        methodBuilder.addStatement("$T $L = new $T()", EvaluationContext.class, evaluationCtxVarName, StandardEvaluationContext.class);
        for (VariableElement parameter : parameters) {
            methodBuilder.addStatement("$L.setVariable($S, $L)", evaluationCtxVarName, parameter.getSimpleName(), parameter.getSimpleName());
        }

        methodBuilder.addStatement("$T $L = $L.getValue($L, $T.class)", String.class, keyStrVarName, expressionField.name, evaluationCtxVarName, String.class);
        methodBuilder.addStatement("$L = getExtensionName($L, $L, $S)", keyStrVarName, keyStrVarName, adaptiveMeta.isUseDefault(), adaptiveMeta.getDefaultExtension());

        methodBuilder.beginControlFlow("if ($L == null)", keyStrVarName);
        methodBuilder.addStatement("throw new $T($T.class)", MismatchExtensionException.class, interfaze);
        methodBuilder.endControlFlow();

        methodBuilder.addStatement("$L $L = this.registry.get($L)", interfaze.getSimpleName(), extensionVarName, keyStrVarName);

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
