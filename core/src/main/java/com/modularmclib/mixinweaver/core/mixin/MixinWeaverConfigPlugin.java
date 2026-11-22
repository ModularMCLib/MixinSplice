package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.api.condition.ConditionResult;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.core.condition.ConditionEvaluator;
import com.modularmclib.mixinweaver.core.extension.ExtensionRegistry;
import com.modularmclib.mixinweaver.core.platform.PlatformEnvironment;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.WeakHashMap;

/** 在配置中指定此插件即可启用 MixinWeaver 的选择条件。 */
public class MixinWeaverConfigPlugin implements IMixinConfigPlugin {

    private static final String CONDITIONAL_DESCRIPTOR = Type.getDescriptor(Conditional.class);
    private final Map<ClassNode, Optional<AnnotationNode>> originalConditions = new WeakHashMap<>();

    private static final class Runtime {

        private static final ExtensionRegistry EXTENSIONS = ExtensionRegistry.discover();
        private static final PlatformEnvironment PLATFORM = ServiceLoader
                .load(PlatformEnvironment.class, PlatformEnvironment.class.getClassLoader())
                .findFirst().orElse((modId, version) -> ConditionResult.UNKNOWN);
        private static final ConditionEvaluator CONDITIONS = new ConditionEvaluator(PLATFORM, EXTENSIONS.conditions(), MixinBytecode::metadata);
        private static final MixinPreparation PREPARATION = new MixinPreparation(CONDITIONS, PLATFORM, EXTENSIONS.targetProviders());
    }

    @Override
    public void onLoad(String mixinPackage) {
        MixinWeaverExtension.install(Runtime.EXTENSIONS);
    }

    @Override
    public @Nullable String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return Runtime.CONDITIONS.shouldApply(targetClassName, mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        Runtime.PREPARATION.prepare(myTargets, otherTargets);
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (Annotations.getInvisible(mixinInfo.getClassNode(ClassReader.SKIP_CODE), Conditional.class) != null) {
            originalConditions.computeIfAbsent(targetClass,
                    node -> Optional.ofNullable(Annotations.getInvisible(node, Conditional.class)));
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Optional<AnnotationNode> original = originalConditions.remove(targetClass);
        if (original == null) {
            return;
        }
        if (targetClass.invisibleAnnotations == null) {
            targetClass.invisibleAnnotations = new ArrayList<>();
        }
        targetClass.invisibleAnnotations.removeIf(annotation -> CONDITIONAL_DESCRIPTOR.equals(annotation.desc));
        original.ifPresent(targetClass.invisibleAnnotations::add);
    }
}
