package com.daiyc.extension.core;

import com.daiyc.extension.util.ExtensionNamingUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author daiyc
 * @since 2024/12/8
 */

public abstract class BaseAdaptiveExtension<EXT> implements AdaptiveExtension {
    protected final ExtensionContext extensionContext;

    protected final Class<EXT> extPointType;

    protected final ExtensionLoader<EXT> extensionLoader;

    /**
     * 候选扩展名称，为空表示不限制
     */
    protected final Set<String> candidateExtensionNames;

    protected final boolean strictMode;

    protected final boolean useDefault;

    /**
     * 默认扩展名，可为空
     */
    protected final String defaultExtension;

    public <E extends Enum<E>>
    BaseAdaptiveExtension(ExtensionContext extensionContext,
                          Class<EXT> extPointType,
                          boolean strictMode,
                          Class<E> enumType, boolean useDefault, String defaultExtension,
                          String... candidateExtensionNames) {

        this.extensionContext = extensionContext;
        this.extPointType = extPointType;
        this.extensionLoader = extensionContext.getExtensionLoader(extPointType);
        this.candidateExtensionNames = Stream.concat(
                        Optional.ofNullable(enumType).map(et -> Arrays.stream(et.getEnumConstants()).map(Enum::name)).orElse(Stream.empty()),
                        Arrays.stream(candidateExtensionNames)
                ).map(e -> {
                    if (strictMode) {
                        return ExtensionNamingUtils.unifyExtensionName(e);
                    }
                    return e;
                }).collect(Collectors.toSet());
        this.strictMode = strictMode;
        this.useDefault = useDefault;
        this.defaultExtension = defaultExtension;
    }

    protected EXT getExtension(String name) {
        return extensionLoader.getExtension(name);
    }

    protected String getExtensionName(String name, boolean useDefault) {
        return getExtensionName(name, useDefault, null);
    }

    protected String getExtensionName(String name, boolean useDefault, String defaultExtension) {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }

        if (useDefault || this.useDefault) {
            return StringUtils.isNotBlank(defaultExtension) ? defaultExtension : this.defaultExtension;
        }
        return null;
    }
}
