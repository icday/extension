package com.daiyc.extension.boot;

import com.daiyc.extension.boot.annotations.EnableExtension;
import lombok.SneakyThrows;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Objects;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * @author daiyc
 * @since 2024/8/4
 */
public class AdaptiveExtensionRegistrar implements ImportBeanDefinitionRegistrar {
    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> annotationAttributes = annotationMetadata.getAnnotationAttributes(EnableExtension.class.getName());
        String ctxBeanName = (String) Objects.requireNonNull(annotationAttributes).get("contextBeanName");

        registerExtensionContext(registry, ctxBeanName);
    }

    protected static void registerExtensionContext(BeanDefinitionRegistry registry, String ctxBeanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionContextBean.class);
        builder.setRole(ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(ctxBeanName, builder.getBeanDefinition());
    }
}
