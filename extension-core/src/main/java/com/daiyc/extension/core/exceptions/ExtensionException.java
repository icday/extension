package com.daiyc.extension.core.exceptions;

import lombok.ToString;

/**
 * @author daiyc
 * @since 2024/8/3
 */
@ToString(callSuper = true)
public abstract class ExtensionException extends RuntimeException {
    protected Class<?> exceptionPointClass;

    public ExtensionException() {
    }

    public ExtensionException(Class<?> exceptionPointClass) {
        this.exceptionPointClass = exceptionPointClass;
    }

    public ExtensionException(String message) {
        super(message);
    }

    public ExtensionException(Class<?> exceptionPointClass, String message, Object... args) {
        super(String.format(exceptionPointClass.getSimpleName() + ".java -> " + message, args));
        this.exceptionPointClass = exceptionPointClass;
    }
}
