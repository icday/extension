package com.daiyc.extension.boot;

import com.daiyc.extension.core.ExtensionContext;
import com.daiyc.extension.core.annotations.ExtensionPoint;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * @author daiyc
 * @since 2024/8/4
 */
public class ExtensionBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {
    @Setter
    private ExtensionContext extensionContext;

    public ExtensionBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    public void registerFilter() {
        this.addIncludeFilter(new AnnotationTypeFilter(ExtensionPoint.class));
        // exclude package-info.java
        addExcludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            return className.endsWith("package-info");
        });
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);

        processBeanDefinitions(beanDefinitionHolders);
        return beanDefinitionHolders;
    }

    @SneakyThrows
    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        AbstractBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (AbstractBeanDefinition) holder.getBeanDefinition();

            String beanClassName = definition.getBeanClassName();

            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
            definition.getPropertyValues().add("extensionPointType", Thread.currentThread().getContextClassLoader().loadClass(beanClassName));
            definition.getPropertyValues().add("extensionContext", extensionContext);
            definition.setBeanClass(ExtensionFactoryBean.class);
            definition.setAttribute("factoryBeanObjectType", beanClassName);
        }
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }
}
