package com.daiyc.extension.processor;

import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author daiyc
 * @since 2024/9/16
 */
public class Scope {
    private final Map<String, AtomicInteger> variables = new ConcurrentHashMap<>();

    public static Scope fromFunction(List<? extends VariableElement> parameters) {
        Scope scope = new Scope();
        parameters.stream()
                .map(VariableElement::getSimpleName)
                .map(Object::toString)
                .forEach(scope::newVar);
        return scope;
    }

    public String newVar(String prefix) {
        int i = variables.computeIfAbsent(prefix, p -> new AtomicInteger(-1))
                .incrementAndGet();
        if (i == 0) {
            return prefix;
        }
        return String.format("%s%d", prefix, i);
    }
}
