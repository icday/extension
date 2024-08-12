package com.daiyc.extension.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author daiyc
 * @since 2024/7/27
 */
public abstract class ExtensionNamingUtils {
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

    public static String unifyExtensionName(String name) {
        name = StringUtils.trimToEmpty(name);
        if (name.isEmpty()) {
            return name;
        }

        if (StringUtils.containsAny(name, '-', '_')) {
            return name.replace('-', '_').toLowerCase();
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = name.length();
        while (i < len) {
            char ch = name.charAt(i);
            if (CharUtils.isAsciiAlphaUpper(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
                i++;

                // 如果后续两个都是大写字母或这是最后一个大写字母
                while (isAsciiAlphaUpper(name, i, len) && (i == len - 1 || isAsciiAlphaUpper(name, i + 1, len))) {
                    sb.append(Character.toLowerCase(name.charAt(i++)));
                }
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    protected static boolean isAsciiAlphaUpper(String src, int index, int len) {
        return index < len && CharUtils.isAsciiAlphaUpper(src.charAt(index));
    }

    private static int readWhile(String source, int i, int len,
                                 StringBuilder sb, Predicate<Character> p, Function<Character, Character> mapper) {
        char ch;
        while (i < len && p.test(ch = source.charAt(i))) {
            sb.append(mapper.apply(ch));
            i++;
        }
        return i;
    }
}
