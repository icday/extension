package com.daiyc.extension.util;

import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * @author daiyc
 * @since 2024/7/28
 */
public abstract class PropertyRetrieveUtils {
    @SneakyThrows
    public static String retrieveKey(Object param, String path) {
        if (path == null || path.isEmpty()) {
            return param.toString();
        }
        Object property = PropertyUtils.getProperty(param, path);
        if (property instanceof String) {
            return (String) property;
        } else if (property instanceof Enum) {
            return ((Enum<?>) property).name();
        } else {
            return property.toString();
        }
    }
}
