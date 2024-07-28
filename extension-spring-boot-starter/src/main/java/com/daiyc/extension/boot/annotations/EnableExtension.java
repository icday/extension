package com.daiyc.extension.boot.annotations;

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
@Import(ExtensionBootstrap.class)
public @interface EnableExtension {
}
