package com.daiyc.extension.boot;

import com.daiyc.extension.core.ExtensionContext;
import com.daiyc.extension.core.annotations.Extension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author daiyc
 * @since 2024/7/28
 */
@Configuration
public class ExtensionBootstrap implements ApplicationContextAware, InitializingBean {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(Extension.class);
        ExtensionContext extensionContext = extensionContext();

        for (Object bean : beanMap.values()) {
            extensionContext.register((Class) bean.getClass(), bean);
        }
    }

    @Bean
    public ExtensionContext extensionContext() {
        return new ExtensionContext();
    }
}
