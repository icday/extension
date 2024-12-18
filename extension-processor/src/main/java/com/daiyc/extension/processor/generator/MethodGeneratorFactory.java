package com.daiyc.extension.processor.generator;

import com.daiyc.extension.core.annotations.Adaptive;

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
        } else {
            return new UnsupportedMethodGenerator(classGenerator, method);
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
