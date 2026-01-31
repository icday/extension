package com.daiyc.extension.processor.generator;

import com.daiyc.extension.core.AdaptiveExtension;
import com.daiyc.extension.core.BaseAdaptiveExtension;
import com.daiyc.extension.core.ExtensionContext;
import com.daiyc.extension.core.ExtensionRegistry;
import com.daiyc.extension.core.annotations.ExtensionPoint;
import com.daiyc.extension.processor.AnnotationUtils;
import com.daiyc.extension.processor.Scope;
import com.daiyc.extension.processor.generator.method.MethodGeneratorFactory;
import com.daiyc.extension.processor.meta.ExtensionPointMeta;
import com.daiyc.extension.util.ExtensionNamingUtils;
import com.squareup.javapoet.*;
import io.vavr.collection.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
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
    @Getter
    private final GenerateContext context;

    @Getter
    private final TypeElement interfaze;

    protected final Scope classScope = new Scope();

    protected List<MethodGenerator> methodGenerators = new ArrayList<>();

    protected final Map<MethodGenerator, String> retrieveMethodGenerators = new ConcurrentHashMap<>();

    @Getter
    protected final ExtensionPointMeta extensionPointMeta;

    protected final TypeName typeName;

    protected final List<FieldSpec> fields = new ArrayList<>();

    public AdaptiveClassGenerator(GenerateContext context, TypeElement interfaze) {
        this.context = context;
        this.interfaze = interfaze;

        this.typeName = ClassName.get(context.getElementUtils().getPackageOf(interfaze).toString()
                , ExtensionNamingUtils.generateAdaptiveSimpleClassName(interfaze.getSimpleName().toString()));

        this.extensionPointMeta = AnnotationUtils.getAnnotationValues(interfaze, ExtensionPoint.class, AnnotationUtils::readExtensionPoint);
    }

    public String registerRetrieveMethod(MethodGenerator methodGenerator) {
        return retrieveMethodGenerators.computeIfAbsent(methodGenerator, m -> classScope.newVar("retrieve"));
    }

    public void addField(FieldSpec matcher) {
        fields.add(matcher);
    }

    public boolean hasField(String name) {
        return fields.stream().anyMatch(f -> f.name.equals(name));
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    @Override
    public void preGenerate() {
        List<ExecutableElement> allMethods = getAllInterfaceMethods();
        this.methodGenerators = Stream.ofAll(allMethods)
                .zipWithIndex((m, i) -> MethodGeneratorFactory.create(this, m, i))
                .toJavaList();

        methodGenerators.forEach(MethodGenerator::preGenerate);

        retrieveMethodGenerators.keySet().forEach(MethodGenerator::preGenerate);
    }

    @Override
    public TypeSpec generate() {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(ExtensionNamingUtils.generateAdaptiveSimpleClassName(interfaze.getSimpleName().toString()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(ClassName.get(BaseAdaptiveExtension.class), ClassName.get(interfaze)))
                .addSuperinterface(interfaze.asType())
                .addSuperinterface(ClassName.get(AdaptiveExtension.class));

        ClassName component = ClassName.bestGuess("org.springframework.stereotype.Component");
        ClassName primary = ClassName.bestGuess("org.springframework.context.annotation.Primary");
        classBuilder.addAnnotation(component);
        classBuilder.addAnnotation(primary);

        for (FieldSpec field : fields) {
            classBuilder.addField(field);
        }

        // region constructor
        ParameterizedTypeName registryType = ParameterizedTypeName.get(ClassName.get(ExtensionRegistry.class), ClassName.get(interfaze));

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ExtensionContext.class, "extensionContext");
//                .addParameter(registryType, "registry");

        CodeBlock.Builder superStatement = CodeBlock.builder();
        superStatement.add("super(extensionContext");
        superStatement.add(", $T.class", interfaze);
        superStatement.add(", $L", extensionPointMeta.isStrictMode());
        if (extensionPointMeta.getEnumType() != null) {
            superStatement.add(", $T.class", extensionPointMeta.getEnumType());
        } else {
            superStatement.add(", null");
        }

        superStatement.add(", $L", extensionPointMeta.isUseDefault());

        if (StringUtils.isNotBlank(extensionPointMeta.getValue())) {
            superStatement.add(", $S", extensionPointMeta.getValue());
        } else {
            superStatement.add(", null");
        }

        if (CollectionUtils.isNotEmpty(extensionPointMeta.getCandidates())) {
            extensionPointMeta.getCandidates()
                    .forEach(name -> superStatement.add(", $S", name));
        }

        superStatement.add(")");
        constructor.addStatement(superStatement.build());
        // endregion
        classBuilder.addMethod(constructor.build());

        Stream.ofAll(methodGenerators)
                .appendAll(retrieveMethodGenerators.keySet())
                .forEach(mg -> classBuilder.addMethod(mg.generate()));

        return classBuilder.build();
    }

    /**
     * 获取所有需要实现的方法
     */
    protected List<ExecutableElement> getAllInterfaceMethods() {
        return ElementFilter.methodsIn(context.getElementUtils().getAllMembers(interfaze))
                .stream()
                .filter(m -> !m.getEnclosingElement().equals(context.getObjectTypeElement()))
                .filter(m -> !m.getModifiers().contains(Modifier.DEFAULT))
                .collect(Collectors.toList());
    }
}
