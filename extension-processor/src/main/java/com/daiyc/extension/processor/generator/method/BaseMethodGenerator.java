package com.daiyc.extension.processor.generator.method;

import com.daiyc.extension.processor.Scope;
import com.daiyc.extension.processor.generator.AdaptiveClassGenerator;
import com.daiyc.extension.processor.generator.GenerateContext;
import com.daiyc.extension.processor.generator.MethodGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/12/15
 */
public abstract class BaseMethodGenerator implements MethodGenerator {
    protected final AdaptiveClassGenerator classGenerator;

    protected final TypeElement interfaze;

    protected final GenerateContext context;

    protected final ExecutableElement method;

    protected final List<? extends VariableElement> parameters;

    protected final int index;

    protected final Scope scope;

    public BaseMethodGenerator(AdaptiveClassGenerator classGenerator, ExecutableElement method, int index) {
        this.classGenerator = classGenerator;
        this.interfaze = classGenerator.getInterfaze();
        this.context = classGenerator.getContext();
        this.method = method;
        this.parameters = method.getParameters();
        this.scope = Scope.fromFunction(this.parameters);
        this.index = index;
    }

    @Override
    public void preGenerate() {
    }

    protected MethodSpec.Builder newMethodBuilder(TypeElement interfaze, ExecutableElement method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(method.getReturnType()));

        List<? extends VariableElement> parameters = method.getParameters();

        for (VariableElement parameter : parameters) {
            TypeMirror parameterType = parameter.asType();
            if (parameterType instanceof TypeVariable) {
                context.getMessager().printMessage(Diagnostic.Kind.NOTE, parameter + " is TypeVariable");
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
            return context.getTypeUtils().asMemberOf(declaredType, typeVariable.asElement());
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
