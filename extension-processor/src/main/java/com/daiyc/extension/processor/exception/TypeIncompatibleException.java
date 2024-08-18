package com.daiyc.extension.processor.exception;

import javax.lang.model.element.Element;

/**
 * @author daiyc
 * @since 2024/8/18
 */
public class TypeIncompatibleException extends BaseCompileException {
    public TypeIncompatibleException() {
    }

    public TypeIncompatibleException(String message) {
        super(message);
    }

    public TypeIncompatibleException(Element element) {
        super(element);
    }

    public TypeIncompatibleException(String message, Element element) {
        super(message, element);
    }
}
