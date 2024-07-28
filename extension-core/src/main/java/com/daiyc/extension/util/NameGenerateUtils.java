package com.daiyc.extension.util;

/**
 * @author daiyc
 * @since 2024/7/27
 */
public abstract class NameGenerateUtils {
    protected static final String POSTFIX = "$Adaptive$Impl0";

    public static String generateAdaptiveFullClassName(Class<?> clazz) {
        return generateAdaptiveFullClassName(clazz.getName());
    }

    public static String generateAdaptiveFullClassName(String className) {
        return className + POSTFIX;
    }

    public static String generateAdaptiveSimpleClassName(Class<?> clazz) {
        return generateAdaptiveFullClassName(clazz.getName());
    }

    public static String generateAdaptiveSimpleClassName(String className) {
        return className + POSTFIX;
    }
}
