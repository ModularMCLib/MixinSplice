package com.modularmc.mixinsplice.gametest.plugin;

import com.modularmc.mixinsplice.api.condition.ConditionSpec;
import com.modularmc.mixinsplice.api.condition.ConditionSpec.Kind;
import com.modularmc.mixinsplice.api.condition.Conditional;
import com.modularmc.mixinsplice.api.target.DynamicTargets;
import com.modularmc.mixinsplice.gametest.target.PluginTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec(kind = Kind.CUSTOM, value = "mixinsplice-test:available"))
@DynamicTargets(provider = "mixinsplice-test:overrides")
@Mixin(PluginTarget.class)
abstract class PluginScopeMixin {

    @Inject(method = "original", at = @At("RETURN"), cancellable = true)
    private void replaceAmount(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(20);
    }
}
