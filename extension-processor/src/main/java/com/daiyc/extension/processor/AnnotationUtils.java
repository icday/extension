package com.daiyc.extension.processor;

import com.daiyc.extension.core.annotations.Adaptive;
import com.daiyc.extension.core.enums.DegradationStrategy;
import com.daiyc.extension.processor.meta.AdaptiveMeta;
import com.daiyc.extension.processor.meta.ExtensionPointMeta;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/7/30
 */
@SuppressWarnings("unchecked")
abstract class AnnotationUtils {

    public static Map<String, AnnotationValue> getAnnotationValues(Element param, Class<?> annClass) {
        AnnotationMirror annotationMirror = param.getAnnotationMirrors()
                .stream()
                .filter(ann -> {
                    TypeElement annElement = (TypeElement) ann.getAnnotationType().asElement();
                    return annClass.getCanonicalName().equals(annElement.getQualifiedName().toString());
                })
                .findFirst()
                .orElse(null);

        return getAnnotationValues(annotationMirror);
    }

    public static Map<String, AnnotationValue> getAnnotationValues(AnnotationMirror annotationMirror) {
        Map<String, AnnotationValue> defaultValues = new HashMap<>();
        if (annotationMirror != null) {
            defaultValues = annotationMirror.getAnnotationType().asElement().getEnclosedElements()
                    .stream()
                    .filter(el -> el.getKind() == ElementKind.METHOD)
                    .map(el -> (ExecutableElement) el)
                    .filter(el -> el.getDefaultValue() != null)
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

    public static DeclaredType getEnumType(Map<String, AnnotationValue> annotationValueMap, String key) {
        return Optional.ofNullable(annotationValueMap.get(key))
                .map(v -> (DeclaredType) v.getValue())
                .filter(c -> !"com.daiyc.extension.core.enums.None".equals(c.toString()))
                .orElse(null);
    }

    public static ExtensionPointMeta readExtensionPoint(Map<String, AnnotationValue> annotationValues) {
        return new ExtensionPointMeta()
                .setAllowNames(readStrings(annotationValues.get("allowNames")))
                .setUnifyName((boolean) annotationValues.get("unifyName").getValue())
                .setValue((String) annotationValues.get("value").getValue())
                .setEnumType(getEnumType(annotationValues, "enumType"));
    }

    public static AdaptiveMeta readAdaptive(Element param) {
        Map<String, AnnotationValue> annotationValues = getAnnotationValues(param, Adaptive.class);
        TypeMirror converterType = (TypeMirror) annotationValues.get("converter").getValue();
        String path = annotationValues.get("value").getValue().toString();
        String degradationStrategyName = annotationValues.get("degradationStrategy").getValue().toString();
        DegradationStrategy degradationStrategy = DegradationStrategy.valueOf(degradationStrategyName);

        AdaptiveMeta adaptiveMeta = new AdaptiveMeta()
                .setConverter(converterType)
                .setValue(path)
                .setDegradationStrategy(degradationStrategy);

        List<AnnotationMirror> toEnums = (List<AnnotationMirror>) annotationValues.get("toEnum").getValue();
        List<AdaptiveMeta.ToEnumMeta> toEnumMetas = toEnums.stream()
                .map(AnnotationUtils::readToEnum)
                .collect(Collectors.toList());

        List<AnnotationMirror> byTypes = (List<AnnotationMirror>) annotationValues.get("byType").getValue();
        List<AdaptiveMeta.ByTypeMeta> byTypeMetas = byTypes.stream()
                .map(AnnotationUtils::readByType)
                .collect(Collectors.toList());

        List<AnnotationMirror> byPatterns = (List<AnnotationMirror>) annotationValues.get("byPattern").getValue();
        List<AdaptiveMeta.ByPatternMeta> byPatternMetas = byPatterns.stream()
                .map(AnnotationUtils::readByPattern)
                .collect(Collectors.toList());

        return adaptiveMeta
                .setToEnums(toEnumMetas)
                .setByTypes(byTypeMetas)
                .setByPatterns(byPatternMetas);
    }

    protected static AdaptiveMeta.ToEnumMeta readToEnum(AnnotationMirror toEnumAnn) {
        Map<String, AnnotationValue> annotationValues = AnnotationUtils.getAnnotationValues(toEnumAnn);
        DeclaredType enumType = getEnumType(annotationValues, "enumType");
        String byMethod = annotationValues.get("byMethod").getValue().toString();
        String byField = annotationValues.get("byField").getValue().toString();
        boolean byOrdinal = (boolean) annotationValues.get("byOrdinal").getValue();

        return new AdaptiveMeta.ToEnumMeta()
                .setEnumType(enumType)
                .setByMethod(byMethod)
                .setByField(byField)
                .setByOrdinal(byOrdinal);
    }

    protected static AdaptiveMeta.ByTypeMeta readByType(AnnotationMirror byTypeAnn) {
        Map<String, AnnotationValue> annotationValues = AnnotationUtils.getAnnotationValues(byTypeAnn);
        List<DeclaredType> types = ((List<AnnotationValue>) annotationValues.get("type").getValue())
                .stream()
                .map(i -> (DeclaredType) i.getValue())
                .collect(Collectors.toList());

        String name = annotationValues.get("name").getValue().toString();

        return new AdaptiveMeta.ByTypeMeta()
                .setTypes(types)
                .setName(name);
    }

    protected static AdaptiveMeta.ByPatternMeta readByPattern(AnnotationMirror byPatternAnn) {
        Map<String, AnnotationValue> annotationValues = AnnotationUtils.getAnnotationValues(byPatternAnn);
        List<String> patterns = readStrings(annotationValues.get("pattern"));
        String name = annotationValues.get("name").getValue().toString();

        return new AdaptiveMeta.ByPatternMeta()
                .setPatterns(patterns)
                .setName(name);
    }

    protected static List<String> readStrings(AnnotationValue annotationValue) {
        List<AnnotationValue> value = (List<AnnotationValue>) annotationValue.getValue();
        if (value == null) {
            return Collections.emptyList();
        }
        return value.stream()
                .map(i -> i.getValue().toString())
                .collect(Collectors.toList());
    }
}
