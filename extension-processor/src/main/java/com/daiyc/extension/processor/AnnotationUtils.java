package com.daiyc.extension.processor;

import javax.lang.model.element.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/7/30
 */
abstract class AnnotationUtils {

    public static Map<String, AnnotationValue> getAnnotationValues(VariableElement param, Class<?> annClass) {
        // adaptiveParam.getAnnotationMirrors().get(0).getAnnotationType().asElement().getKind()
        AnnotationMirror annotationMirror = param.getAnnotationMirrors()
                .stream()
                .filter(ann -> {
                    TypeElement annElement = (TypeElement) ann.getAnnotationType().asElement();
                    return annClass.getCanonicalName().equals(annElement.getQualifiedName().toString());
                })
                .findFirst()
                .orElse(null);

        Map<String, AnnotationValue> defaultValues = new HashMap<>();
        if (annotationMirror != null) {
            defaultValues = annotationMirror.getAnnotationType().asElement().getEnclosedElements()
                    .stream()
                    .filter(el -> el.getKind() == ElementKind.METHOD)
                    .map(el -> (ExecutableElement) el)
                    .collect(Collectors.toMap(el -> el.getSimpleName().toString(), ExecutableElement::getDefaultValue));
        }

        Map<String, AnnotationValue> annValues = new HashMap<>();
        if (annotationMirror != null) {
            annValues = io.vavr.collection.HashMap.ofAll(annotationMirror.getElementValues())
                    .mapKeys(k -> k.getSimpleName().toString())
                    .mapValues(v -> (AnnotationValue) v)
                    .toJavaMap();
        }

        defaultValues.putAll(annValues);

        return defaultValues;
    }
}
