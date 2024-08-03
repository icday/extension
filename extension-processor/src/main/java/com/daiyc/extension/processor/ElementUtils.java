package com.daiyc.extension.processor;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author daiyc
 * @since 2024/8/3
 */
abstract class ElementUtils {

    public static Tuple2<VariableElement, ExecutableElement> findProperty(DeclaredType type, String propertyName) {
        List<? extends Element> members = type.asElement().getEnclosedElements();

        VariableElement field = Stream.ofAll(ElementFilter.fieldsIn(members))
                .find(f -> f.getSimpleName().toString().equals(propertyName))
                .getOrNull();

        if (field == null) {
            return null;
        }

        List<String> prefixes = Arrays.asList("is", "get");
        ExecutableElement getter = Stream.ofAll(ElementFilter.methodsIn(members))
                .map(method -> {
                    String name = method.getSimpleName().toString();
                    for (String prefix : prefixes) {
                        if (!StringUtils.startsWith(name, prefix)) {
                            continue;
                        }
                        String propName = StringUtils.uncapitalize(StringUtils.removeStart(name, prefix));
                        return Tuple.of(propName, method);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(t -> t._1.equals(propertyName))
                .map(t -> t._2)
                .getOrNull();

        return Tuple.of(field, getter);
    }
}
