package com.daiyc.extension.boot;

import com.daiyc.extension.boot.annotations.EnableExtension;
import lombok.SneakyThrows;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.*;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * @author daiyc
 * @since 2024/8/4
 */
public class AdaptiveExtensionRegistrar implements ImportBeanDefinitionRegistrar {
    @SneakyThrows
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        List<String> packages = Optional.of(annotationMetadata.getAnnotationAttributes(EnableExtension.class.getName()))
                .map(attrs -> (String[]) attrs.get("scanPackages"))
                .map(Arrays::asList)
                .orElseGet(() -> Optional.ofNullable(annotationMetadata.getAnnotationAttributes(ComponentScan.class.getName()))
                        .map(attrs -> (String[]) attrs.get("basePackages"))
                        .map(Arrays::asList)
                        .orElse(Collections.emptyList()));

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionScannerConfigurer.class);
        builder.addPropertyValue("scanPackages", packages);
        builder.addPropertyReference("extensionContext", "extensionContext");

        builder.setRole(ROLE_INFRASTRUCTURE);
        BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
    }
}
