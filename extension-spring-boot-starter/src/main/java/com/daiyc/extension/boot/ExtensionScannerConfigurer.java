package com.daiyc.extension.boot;

import com.daiyc.extension.core.ExtensionContext;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;

/**
 * @author daiyc
 * @since 2024/8/5
 */
public class ExtensionScannerConfigurer implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
    @Setter
    private List<String> scanPackages;

    @Setter
    private ExtensionContext extensionContext;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @SneakyThrows
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ExtensionBeanDefinitionScanner scanner = new ExtensionBeanDefinitionScanner(registry);
        scanner.setExtensionContext(extensionContext);
        scanner.setResourceLoader(applicationContext);
        scanner.registerFilter();

        scanner.scan(scanPackages.toArray(new String[]{}));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
