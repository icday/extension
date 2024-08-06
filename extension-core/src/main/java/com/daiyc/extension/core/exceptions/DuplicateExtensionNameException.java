package com.daiyc.extension.core.exceptions;

/**
 * @author daiyc
 * @since 2024/8/4
 */
public class DuplicateExtensionNameException extends ExtensionException {
    public DuplicateExtensionNameException() {
    }

    public DuplicateExtensionNameException(Class<?> exceptionPointClass) {
        super(exceptionPointClass);
    }

    public DuplicateExtensionNameException(String message) {
        super(message);
    }

    public DuplicateExtensionNameException(Class<?> exceptionPointClass, String message, Object... args) {
        super(exceptionPointClass, message, args);
    }
}
