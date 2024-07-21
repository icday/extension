package com.daiyc.extension.example;

import com.daiyc.extension.core.annotations.Adaptive;
import com.daiyc.extension.core.annotations.ExtensionPoint;

/**
 * @author daiyc
 * @since 2024/7/20
 */
@ExtensionPoint("foo")
public interface Strategy {
    String getType(@Adaptive String type);

    String show(@Adaptive("name") Arguments args);
}
