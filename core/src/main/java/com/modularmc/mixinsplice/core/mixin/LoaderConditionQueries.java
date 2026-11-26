package com.modularmc.mixinsplice.core.mixin;

import com.modularmc.mixinsplice.api.condition.ConditionQueries;

import java.util.Optional;

/** 将条件查询委托给运行时使用的求值器和平台。 */
public final class LoaderConditionQueries implements ConditionQueries {

    @Override
    public Decision check(String target, String mixin) {
        return MixinSpliceConfigPlugin.conditions().check(target, mixin);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return MixinSpliceConfigPlugin.platform().modVersion(modId);
    }

    @Override
    public boolean matchesVersion(String version, String predicate) {
        return MixinSpliceConfigPlugin.platform().matchesVersion(version, predicate);
    }
}
