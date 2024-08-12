package com.daiyc.extension.boot;

import com.daiyc.extension.boot.annotations.EnableExtension;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * @author daiyc
 * @since 2024/8/4
 */
public class AdaptiveExtensionRegistrar implements ImportBeanDefinitionRegistrar {
    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        String ctxBeanName = (String) annotationMetadata.getAnnotationAttributes(EnableExtension.class.getName())
                .get("contextBeanName");

        registerExtensionContext(annotationMetadata, registry, ctxBeanName);
        registerConfigurer(annotationMetadata, registry, ctxBeanName);
    }

    protected static void registerExtensionContext(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry, String ctxBeanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionContextBean.class);
        builder.setRole(ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(ctxBeanName, builder.getBeanDefinition());
    }

    private static void registerConfigurer(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry, String extensionCtxBeanName) {
        List<String> packages = Optional.ofNullable(annotationMetadata.getAnnotationAttributes(EnableExtension.class.getName()))
                .map(attrs -> (String[]) attrs.get("scanPackages"))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());

        if (CollectionUtils.isEmpty(packages)) {
            packages = Optional.ofNullable(annotationMetadata.getAnnotationAttributes(ComponentScan.class.getName()))
                    .map(attrs -> (String[]) attrs.get("basePackages"))
                    .map(Arrays::asList)
                    .orElse(Collections.emptyList());
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionScannerConfigurer.class);
        builder.addPropertyValue("scanPackages", packages);
        builder.addPropertyReference("extensionContext", extensionCtxBeanName);

        builder.setRole(ROLE_INFRASTRUCTURE);
        BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
    }
}
