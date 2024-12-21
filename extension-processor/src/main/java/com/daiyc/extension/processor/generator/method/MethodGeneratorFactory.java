package com.daiyc.extension.processor.generator.method;

import com.daiyc.extension.core.annotations.Adaptive;
import com.daiyc.extension.processor.generator.AdaptiveClassGenerator;
import com.daiyc.extension.processor.generator.MethodGenerator;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/12/15
 */
public abstract class MethodGeneratorFactory {
    public static MethodGenerator create(AdaptiveClassGenerator classGenerator, ExecutableElement method, int index) {
        VariableElement adaptiveParam = getAdaptiveParam(method);

        if (adaptiveParam != null) {
            return new AdaptiveMethodGenerator(classGenerator, method, index, adaptiveParam);
        } else if (method.getAnnotation(Adaptive.class) != null) {
            return new SPELMethodGenerator(classGenerator, method, index);
        } else {
            return new UnsupportedMethodGenerator(classGenerator, method, index);
        }
    }

    protected static VariableElement getAdaptiveParam(ExecutableElement method) {
        int idx = getAdaptiveParamIndex(method);
        if (idx < 0) {
            return null;
        }
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
