package com.daiyc.extension.processor.meta;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.lang.model.type.DeclaredType;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/9/18
 */
@Data
@Accessors(chain = true)
public class ExtensionPointMeta {
    private String value;

    private DeclaredType enumType;

    private List<String> allowNames;

    private boolean unifyName;
}
