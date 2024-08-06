package com.daiyc.extension.boot.annotations;

import com.daiyc.extension.boot.AdaptiveExtensionRegistrar;
import com.daiyc.extension.boot.ExtensionBootstrap;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author daiyc
 * @since 2024/7/28
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ExtensionBootstrap.class, AdaptiveExtensionRegistrar.class})
public @interface EnableExtension {
    /**
     * 指定扫描扩展点的包路径 <br/>
     * 如果未指定则尝试使用Spring（ComponentScan）的扫描路径
     */
    String[] scanPackages() default {};
}
