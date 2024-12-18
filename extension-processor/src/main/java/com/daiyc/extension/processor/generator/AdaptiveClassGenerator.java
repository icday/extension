package com.daiyc.extension.processor.generator;

import com.daiyc.extension.core.AdaptiveExtension;
import com.daiyc.extension.core.BaseAdaptiveExtension;
import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.annotations.ExtensionPoint;
import com.daiyc.extension.processor.AnnotationUtils;
import com.daiyc.extension.processor.Scope;
import com.daiyc.extension.processor.meta.ExtensionPointMeta;
import com.daiyc.extension.util.ExtensionNamingUtils;
import com.squareup.javapoet.*;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author daiyc
 * @since 2024/7/31
 */
@EqualsAndHashCode(of = "typeName")
@SuppressWarnings("unchecked")
public class AdaptiveClassGenerator implements ClassGenerator {
    final ProcessingEnvironment processingEnv;

    final TypeElement interfaze;

    final Elements elementUtils;

    final Types typeUtils;

    final TypeElement objectTypeElement;

    protected final Map<Tuple2<String, String>, MethodSpec> helpMethods = new HashMap<>();

    protected final Scope classScope = new Scope();

    protected TypeSpec cache = null;

    protected List<MethodGenerator> methodGenerators = new ArrayList<>();

    protected final Map<MethodGenerator, String> retrieveMethodGenerators = new ConcurrentHashMap<>();

    @Getter
    protected final ExtensionPointMeta extensionPointMeta;

    protected final TypeName typeName;

    protected final List<FieldSpec> fields = new ArrayList<>();

    public AdaptiveClassGenerator(ProcessingEnvironment processingEnv, TypeElement interfaze) {
        this.processingEnv = processingEnv;
        this.interfaze = interfaze;
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.objectTypeElement = elementUtils.getTypeElement("java.lang.Object");

        this.typeName = ClassName.get(elementUtils.getPackageOf(interfaze).toString()
                , ExtensionNamingUtils.generateAdaptiveSimpleClassName(interfaze.getSimpleName().toString()));

        this.extensionPointMeta = AnnotationUtils.getAnnotationValues(interfaze, ExtensionPoint.class, AnnotationUtils::readExtensionPoint);
    }

    String registerRetrieveMethod(MethodGenerator methodGenerator) {
        return retrieveMethodGenerators.computeIfAbsent(methodGenerator, m -> classScope.newVar("retrieve"));
    }

    void registerMatcher(FieldSpec matcher) {
        fields.add(matcher);
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    @Override
    public boolean preGenerate(GenerateContext ctx) {
        List<ExecutableElement> allMethods = getAllInterfaceMethods();
        this.methodGenerators = Stream.ofAll(allMethods)
                .zipWithIndex((m, i) -> MethodGeneratorFactory.create(this, m, i))
                .toJavaList();

        methodGenerators.forEach(mg -> mg.preGenerate(ctx));

        retrieveMethodGenerators.keySet().forEach(mg -> mg.preGenerate(ctx));

        return true;
    }

    @Override
    public TypeSpec generate() {
        if (cache != null) {
            return cache;
        }

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(ExtensionNamingUtils.generateAdaptiveSimpleClassName(interfaze.getSimpleName().toString()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(ClassName.get(BaseAdaptiveExtension.class), ClassName.get(interfaze)))
                .addSuperinterface(interfaze.asType())
                .addSuperinterface(ClassName.get(AdaptiveExtension.class));

        for (FieldSpec field : fields) {
            classBuilder.addField(field);
        }

        // region constructor
        ParameterizedTypeName registryType = ParameterizedTypeName.get(ClassName.get(ExtensionRegistry.class), ClassName.get(interfaze));

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(registryType, "registry");

        CodeBlock.Builder superStatement = CodeBlock.builder();
        superStatement.add("super(registry");
        if (extensionPointMeta.getEnumType() != null) {
            superStatement.add(", $T.class", extensionPointMeta.getEnumType());
        } else {
            superStatement.add(", null");
        }

        if (StringUtils.isNotBlank(extensionPointMeta.getValue())) {
            superStatement.add(", $S", extensionPointMeta.getValue());
        } else {
            superStatement.add(", null");
        }

        if (CollectionUtils.isNotEmpty(extensionPointMeta.getAllowNames())) {
            extensionPointMeta.getAllowNames()
                    .forEach(name -> superStatement.add(", $S", name));
        }

        superStatement.add(")");
        constructor.addStatement(superStatement.build());
        // endregion
        classBuilder.addMethod(constructor.build());

        return cache = classBuilder.build();
    }

    /**
     * 获取所有需要实现的方法
     */
    protected List<ExecutableElement> getAllInterfaceMethods() {
        return ElementFilter.methodsIn(elementUtils.getAllMembers(interfaze))
                .stream()
                .filter(m -> !m.getEnclosingElement().equals(objectTypeElement))
                //                .filter(m -> !m.getModifiers().contains(Modifier.DEFAULT))
                .collect(Collectors.toList());
    }
}
