package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.api.condition.ConditionResult;
import com.modularmclib.mixinweaver.core.condition.ConditionEvaluator;
import com.modularmclib.mixinweaver.core.extension.ExtensionRegistry;
import com.modularmclib.mixinweaver.core.platform.PlatformEnvironment;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/** 由 MixinWeaver 自带配置加载，为当前平台的 Mixin 安装扩展。 */
public class MixinWeaverConfigPlugin implements IMixinConfigPlugin {

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
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
