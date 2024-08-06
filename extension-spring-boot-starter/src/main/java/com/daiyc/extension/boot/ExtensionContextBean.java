package com.daiyc.extension.boot;

import com.daiyc.extension.core.ExtensionContext;
import com.daiyc.extension.core.annotations.Extension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author daiyc
 * @since 2024/8/6
 */
@Component
public class ExtensionContextBean extends ExtensionContext implements ApplicationContextAware, InitializingBean {
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(Extension.class);

        for (Object bean : beanMap.values()) {
            this.register((Class) bean.getClass(), bean);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
