package com.daiyc.extension.processor.meta;

import com.daiyc.extension.core.enums.DegradationStrategy;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * @author daiyc
 * @since 2024/9/17
 */
@Data
@Accessors(chain = true)
public class AdaptiveMeta {
    private String value;

    private TypeMirror converter;

    private DegradationStrategy degradationStrategy;

    private List<ToEnumMeta> toEnums;

    private List<ByTypeMeta> byTypes;

    private List<ByPatternMeta> byPatterns;

    public void validate() {
        int cnt = 0;
        if (CollectionUtils.isNotEmpty(toEnums)) {
            cnt++;
            if (toEnums.size() > 1) {
                throw new IllegalArgumentException("@Adaptive.toEnum() can ONLY contains ONE element");
            }

            toEnums.forEach(ToEnumMeta::validate);
        }

        if (CollectionUtils.isNotEmpty(byTypes)) {
            cnt++;
            byTypes.forEach(ByTypeMeta::validate);
        }

        if (CollectionUtils.isNotEmpty(byPatterns)) {
            cnt++;
            byPatterns.forEach(ByPatternMeta::validate);
        }

        if (cnt > 1) {
            throw new IllegalArgumentException("@Adaptive MUST ONLY specify one of toEnum(), byType() or byPattern");
        }
    }

    @Data
    @Accessors(chain = true)
    public static class ToEnumMeta {
        private DeclaredType enumType;

        private String byMethod;

        private String byField;

        private boolean byOrdinal;

        public void validate() {
            int cnt = 0;
            if (StringUtils.isNotBlank(byMethod)) {
                cnt++;
            }
            if (StringUtils.isNotBlank(byField)) {
                cnt++;
            }
            if (byOrdinal) {
                cnt++;
            }
            if (cnt == 0) {
                throw new IllegalArgumentException("@ToEnum MUST specify any of byMethod, byField or byOrdinal strategy");
            }

            if (cnt > 1) {
                throw new IllegalArgumentException("@ToEnum MUST ONLY specify one of byMethod, byField or byOrdinal strategy");
            }
        }
    }

    @Data
    @Accessors(chain = true)
    public static class ByTypeMeta {
        private List<DeclaredType> types;

        private String name;

        public void validate() {
            if (CollectionUtils.isEmpty(types)) {
                throw new IllegalArgumentException("Must specify types for @Adaptive.byType");
            }
        }
    }

    @Data
    @Accessors(chain = true)
    public static class ByPatternMeta {
        private List<String> patterns;

        private String name;

        public void validate() {
            if (CollectionUtils.isEmpty(patterns)) {
                throw new IllegalArgumentException("Must specify pattern for @Adaptive.byPattern");
            }
        }
    }
}
