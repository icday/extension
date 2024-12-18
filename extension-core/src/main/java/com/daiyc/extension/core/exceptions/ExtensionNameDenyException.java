package com.daiyc.extension.core.exceptions;

/**
 * @author daiyc
 * @since 2024/12/19
 */
public class ExtensionNameDenyException extends ExtensionException {
    public ExtensionNameDenyException() {
    }

    public ExtensionNameDenyException(Class<?> exceptionPointClass) {
        super(exceptionPointClass);
    }

    public ExtensionNameDenyException(String message) {
        super(message);
    }

    public ExtensionNameDenyException(Class<?> exceptionPointClass, String message, Object... args) {
        super(exceptionPointClass, message, args);
    }
}
