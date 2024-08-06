package com.daiyc.extension.boot;

import com.daiyc.extension.core.ExtensionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author daiyc
 * @since 2024/7/28
 */
@Configuration
public class ExtensionBootstrap {
    @Bean
    public ExtensionContext extensionContext() {
        return new ExtensionContextBean();
    }
}
