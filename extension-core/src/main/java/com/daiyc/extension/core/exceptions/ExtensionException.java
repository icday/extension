package com.daiyc.extension.core.exceptions;

/**
 * @author daiyc
 * @since 2024/8/3
 */
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
        super(String.format(message, args));
        this.exceptionPointClass = exceptionPointClass;
    }
}
