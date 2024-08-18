package com.daiyc.extension.processor.exception;

import javax.lang.model.element.Element;

/**
 * @author daiyc
 * @since 2024/8/18
 */
public abstract class BaseCompileException extends RuntimeException {
    protected Element element;

    public BaseCompileException() {
    }

    public BaseCompileException(String message) {
        super(message);
    }

    public BaseCompileException(Element element) {
        this.element = element;
    }

    public BaseCompileException(String message, Element element) {
        super(message);
        this.element = element;
    }
}
