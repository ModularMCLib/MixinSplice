package com.modularmc.mixinsplice.gametest;

import com.modularmc.mixinsplice.api.condition.Condition;
import com.modularmc.mixinsplice.api.condition.Conditions;
import com.modularmc.mixinsplice.api.extension.SpliceExtension;
import com.modularmc.mixinsplice.api.mixin.MixinCanceller;
import com.modularmc.mixinsplice.api.target.DynamicTargetProvider;
import com.modularmc.mixinsplice.api.transformation.AnnotationTransformation;
import com.modularmc.mixinsplice.api.transformation.InjectionAnnotation;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PluginTestExtension implements SpliceExtension {

    private static final String TEST_MIXIN_PACKAGE = "com.modularmc.mixinsplice.gametest.plugin.";

    @Override
    public String id() {
        return "mixinsplice-test:plugin";
    }

    @Override
    public Map<String, Condition> conditions() {
        return Map.of("mixinsplice-test:available", Conditions.modLoaded("mixinsplice"));
    }

    @Override
    public Map<String, DynamicTargetProvider> targetProviders() {
        return Map.of("mixinsplice-test:overrides", context -> Set.copyOf(context.subclassesOverriding("amount")));
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
