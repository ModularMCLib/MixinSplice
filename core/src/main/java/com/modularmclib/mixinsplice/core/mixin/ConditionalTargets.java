package com.modularmclib.mixinsplice.core.mixin;

import com.modularmclib.mixinsplice.api.condition.Conditional;
import com.modularmclib.mixinsplice.core.condition.ConditionEvaluator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.util.List;

/** 从配置读入的 Mixin 字节码中移除不满足条件的目标。 */
final class ConditionalTargets {

    private final ConditionEvaluator conditions;
    private final ILogger logger = MixinService.getService().getLogger("mixinsplice");

    ConditionalTargets(ConditionEvaluator conditions) {
        this.conditions = conditions;
    }

    ClassNode filter(IMixinConfig config, ClassNode source) {
        if (Annotations.getInvisible(source, Conditional.class) == null) {
            return source;
        }
        AnnotationNode mixin = Annotations.getInvisible(source, Mixin.class);
        if (mixin == null) {
            return source;
        }
        String name = source.name.replace('/', '.');
        List<Type> classes = Annotations.getValue(mixin, "value", true);
        List<String> names = Annotations.getValue(mixin, "targets", true);
        var refmap = MixinInternals.referenceMapper(config);
        List<Type> acceptedClasses = classes.stream().filter(target -> accepts(target.getClassName(), name, source)).toList();
        List<String> acceptedNames = names.stream()
                .filter(target -> accepts(refmap.remap(source.name, target).replace('/', '.'), name, source)).toList();
        if (classes.size() == acceptedClasses.size() && names.size() == acceptedNames.size()) {
            return source;
        }

        // Loader 可能复用 ASM 节点。只修改交给本配置的副本。
        var copy = new ClassNode();
        source.accept(copy);
        AnnotationNode declaration = Annotations.getInvisible(copy, Mixin.class);
        Annotations.setValue(declaration, "value", acceptedClasses);
        Annotations.setValue(declaration, "targets", acceptedNames);
        return copy;
    }

    private boolean accepts(String target, String name, ClassNode source) {
        var decision = conditions.check(target, name, source);
        if (!decision.allowed()) {
            logger.debug("跳过 {} -> {}，{}", name, target, decision.reason());
        }
        return decision.allowed();
    }
}
