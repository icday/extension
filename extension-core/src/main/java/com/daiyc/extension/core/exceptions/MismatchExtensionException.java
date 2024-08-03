package com.daiyc.extension.core.exceptions;

/**
 * @author daiyc
 * @since 2024/8/3
 */
public class MismatchExtensionException extends ExtensionException {
    public MismatchExtensionException() {
    }

    public MismatchExtensionException(Class<?> exceptionPointClass) {
        super(exceptionPointClass);
    }

    public MismatchExtensionException(String message) {
        super(message);
    }

    public MismatchExtensionException(String message, Class<?> exceptionPointClass) {
        super(message, exceptionPointClass);
    }
}
