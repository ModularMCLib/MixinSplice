package com.modularmclib.mixinweaver.gametest;

import com.modularmclib.mixinweaver.api.extension.WeaverExtension;
import com.modularmclib.mixinweaver.api.mixin.MixinCanceller;
import com.modularmclib.mixinweaver.api.transformation.AnnotationTransformation;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.List;

public final class TransformationFixture implements WeaverExtension {

    private static final String MIXIN_PACKAGE = "com.modularmclib.mixinweaver.gametest.mixins.";

    @Override
    public String id() {
        return "mixinweaver-gametest";
    }

    @Override
    public List<MixinCanceller> cancellers() {
        return List.of((mixin, target) -> mixin.className().equals(MIXIN_PACKAGE + "CancelledMixin"));
    }

    @Override
    public List<AnnotationTransformation> transformations() {
        return List.of((target, annotations) -> {
            if (!target.mixin().className().equals(MIXIN_PACKAGE + "OriginMixin") || target.method().stream().noneMatch(method -> method.name().equals("changeAdjusted"))) {
                return annotations;
            }
            return annotations.stream().map(annotation -> annotation.descriptor().equals(Type.getDescriptor(Inject.class)) ? annotation.withValue("method", List.of("adjustedValue")) : annotation).toList();
        });
    }
}
