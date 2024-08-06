package com.daiyc.extension.boot;

import com.daiyc.extension.core.ExtensionContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author daiyc
 * @since 2024/8/5
 */
public class ExtensionFactoryBean<T> implements FactoryBean<T> {
    @Setter
    @Getter
    private Class<T> extensionPointType;

    @Setter
    private ExtensionContext extensionContext;

    public ExtensionFactoryBean() {
    }

    public ExtensionFactoryBean(Class<T> extensionPointType) {
        this.extensionPointType = extensionPointType;
    }

    @Override
    public T getObject() throws Exception {
        return extensionContext.getExtensionLoader(extensionPointType).getExtension();
    }

    @Override
    public Class<?> getObjectType() {
        return extensionPointType;
    }
}
