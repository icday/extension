package com.daiyc.extension.processor.generator.method;

import com.daiyc.extension.processor.ElementUtils;
import com.daiyc.extension.processor.generator.MethodGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/12/17
 */
@EqualsAndHashCode(of = {"type", "path"})
public class RetrieveMethodGenerator implements MethodGenerator {
    private final TypeMirror type;

    private final String path;

    private final List<String> propertyNames;

    @Getter
    private final TypeMirror returnType;

    @Setter
    @Getter
    private String methodName;

    public RetrieveMethodGenerator(TypeMirror type, String path) {
        this.type = type;
        this.path = path;

        this.propertyNames = Arrays.asList(StringUtils.split(path, "."));
        this.returnType = ElementUtils.getDestType(type, propertyNames);
    }

    @Override
    public void preGenerate() {
    }

    @Override
    public MethodSpec generate() {
        TypeMirror returnType = ElementUtils.getDestType(type, propertyNames);

        String baseArg = "arg";
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get(type), baseArg)
                .returns(TypeName.get(returnType));

        Stream.ofAll(propertyNames)
                .foldLeft(Tuple.of(0, baseArg, type), (cur, propName) -> cur.apply((i, varName, varType) -> {
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
}
