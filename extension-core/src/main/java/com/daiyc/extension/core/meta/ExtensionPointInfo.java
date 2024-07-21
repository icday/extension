package com.daiyc.extension.core.meta;

import com.daiyc.extension.core.annotations.Adaptive;
import com.daiyc.extension.core.annotations.ExtensionPoint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/7/21
 */
@Data
@EqualsAndHashCode(of = "type")
public class ExtensionPointInfo {
    /**
     * 扩展点类型
     */
    private final Class<?> type;

    private final String defaultName;

    private final List<MethodInfo> methods;

    public ExtensionPointInfo(Class<?> type) {
        this.type = type;

        this.methods = Arrays.stream(type.getMethods())
                .filter(method -> !method.isDefault())
                .map(MethodInfo::new)
                .collect(Collectors.toList());

        ExtensionPoint ann = type.getAnnotation(ExtensionPoint.class);
        defaultName = ann.value();
    }

    public MethodInfo getMethodInfo(Method method) {
        return methods.stream()
                .filter(methodInfo -> methodInfo.method.equals(method))
                .findFirst()
                .orElse(null);
    }

    @Getter
    @EqualsAndHashCode(of = "method")
    public class MethodInfo {

        protected final Method method;

        private Integer paramIndex;

        private String path;

        public MethodInfo(Method method) {
            this.method = method;

            init();
        }

        protected void init() {
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Adaptive ann = parameter.getAnnotation(Adaptive.class);
                if (ann == null) {
                    continue;
                }
                paramIndex = i;
                path = ann.value();
            }
        }

        public boolean isAdaptive() {
            return paramIndex != null;
        }
    }
}
