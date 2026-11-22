package com.modularmclib.mixinweaver.gametest;

import com.modularmclib.mixinweaver.api.condition.Condition;
import com.modularmclib.mixinweaver.api.condition.Conditions;
import com.modularmclib.mixinweaver.api.extension.WeaverExtension;
import com.modularmclib.mixinweaver.api.mixin.MixinCanceller;
import com.modularmclib.mixinweaver.api.target.DynamicTargetProvider;
import com.modularmclib.mixinweaver.api.transformation.AnnotationTransformation;
import com.modularmclib.mixinweaver.api.transformation.InjectionAnnotation;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PluginTestExtension implements WeaverExtension {

    private static final String TEST_MIXIN_PACKAGE = "com.modularmclib.mixinweaver.gametest.plugin.";

    @Override
    public String id() {
        return "mixinweaver-test:plugin";
    }

    @Override
    public Map<String, Condition> conditions() {
        return Map.of("mixinweaver-test:available", Conditions.modLoaded("mixinweaver"));
    }

    @Override
    public Map<String, DynamicTargetProvider> targetProviders() {
        return Map.of("mixinweaver-test:overrides", context -> Set.copyOf(context.subclassesOverriding("amount")));
    }

    @Override
    public List<MixinCanceller> cancellers() {
        return List.of((mixin, target) -> mixin.className().equals(TEST_MIXIN_PACKAGE + "CancelledPluginMixin"));
    }

    @Override
    public List<AnnotationTransformation> transformations() {
        String inject = Type.getDescriptor(Inject.class);
        return List.of((target, annotations) -> {
            if (!target.mixin().className().startsWith(TEST_MIXIN_PACKAGE + "PluginScopeMixin")) {
                return annotations;
            }
            return annotations.stream().map(annotation -> annotation.descriptor().equals(inject) ? new InjectionAnnotation(annotation).withMethods(List.of("amount")).mapAt(point -> point.withOrdinal(0)).view() : annotation).toList();
        });
    }
}
