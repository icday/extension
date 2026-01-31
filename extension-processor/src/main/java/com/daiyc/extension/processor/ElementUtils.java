package com.daiyc.extension.processor;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author daiyc
 * @since 2024/8/3
 */
public abstract class ElementUtils {
    /**
     * 获取目标类型
     *
     * @param srcType 源类型
     * @param propertyPath 属性路径（或者通过getter方法）
     * @return 目标类型
     */
    public static TypeMirror getNestedType(DeclaredType srcType, List<String> propertyPath) {
        return Stream.ofAll(propertyPath)
                .foldLeft((TypeMirror) srcType, (type0, prop) -> {
                    assert type0.getKind() == TypeKind.DECLARED;
                    DeclaredType declaredType = (DeclaredType) type0;
                    
                    // 先尝试查找直接字段
                    VariableElement field = findField(declaredType, prop);
                    if (field != null) {
                        return field.asType();
                    }
                    
                    // 如果没有找到字段，则尝试查找getter方法
                    ExecutableElement getter = findGetterMethod(declaredType, prop);
                    if (getter != null) {
                        return getter.getReturnType();
                    }
                    
                    // 如果都没有找到，则抛出异常
                    throw new IllegalArgumentException("Cannot find property or getter '" + prop + "' in type '" + type0 + "'");
                });
    }

    /**
     * 获取对应属性的getter
     *
     * @param type 字段所在类
     * @param propertyName 字段名
     * @return (字段类型, getter方法)
     */
    public static Tuple2<TypeMirror, ExecutableElement> findProperty(DeclaredType type, String propertyName) {
        ExecutableElement getter = findGetterMethod(type, propertyName);
        return Tuple.of(getter.getReturnType(), getter);
    }

    private static ExecutableElement findGetterMethod(DeclaredType type, String propertyName) {
        List<? extends Element> members = type.asElement().getEnclosedElements();
        return findGetterMethod(members, propertyName);
    }

    private static ExecutableElement findGetterMethod(List<? extends Element> members, String propertyName) {
        List<String> prefixes = Arrays.asList("is", "get");
        return Stream.ofAll(ElementFilter.methodsIn(members))
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
    }

    public static TypeMirror getDestType(TypeMirror type, List<String> propNames) {
        if (propNames.isEmpty()) {
            return type;
        }
        return ElementUtils.getNestedType((DeclaredType) type, propNames);
    }

    public static ExecutableElement findMethod(DeclaredType type, String methodName, Boolean isStatic) {
        List<? extends Element> members = type.asElement().getEnclosedElements();
        return Stream.ofAll(ElementFilter.methodsIn(members))
                .filter(m -> m.getModifiers().contains(Modifier.PUBLIC))
                .filter(m -> {
                    if (isStatic == null) {
                        return true;
                    }
                    boolean containsStatic = m.getModifiers().contains(Modifier.STATIC);
                    return containsStatic == isStatic;
                })
                .find(m -> m.getSimpleName().toString().equals(methodName))
                .getOrNull();
    }
    public static VariableElement findField(DeclaredType type, String fieldName) {
        List<? extends Element> members = type.asElement().getEnclosedElements();
        return Stream.ofAll(ElementFilter.fieldsIn(members))
                .find(m -> m.getSimpleName().toString().equals(fieldName))
                .getOrNull();
    }
}
