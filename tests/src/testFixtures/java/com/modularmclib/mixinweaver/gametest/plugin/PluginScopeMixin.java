package com.modularmclib.mixinweaver.gametest.plugin;

import com.modularmclib.mixinweaver.api.condition.ConditionSpec;
import com.modularmclib.mixinweaver.api.condition.ConditionSpec.Kind;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.api.target.DynamicTargets;
import com.modularmclib.mixinweaver.gametest.target.PluginTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec(kind = Kind.CUSTOM, value = "mixinweaver-test:available"))
@DynamicTargets(provider = "mixinweaver-test:overrides")
@Mixin(PluginTarget.class)
abstract class PluginScopeMixin {

    @Inject(method = "original", at = @At("RETURN"), cancellable = true)
    private void replaceAmount(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(20);
    }
}
