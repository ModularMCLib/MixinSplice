package com.modularmc.mixinsplice.gametest.plugin;

import com.modularmc.mixinsplice.api.condition.ConditionSpec;
import com.modularmc.mixinsplice.api.condition.Conditional;
import com.modularmc.mixinsplice.gametest.target.PluginTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec("mixinsplice_missing_plugin_mod"))
@Mixin(PluginTarget.class)
abstract class UnavailablePluginMixin {

    @Inject(method = "unavailable", at = @At("RETURN"), cancellable = true)
    private void replaceAmount(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(100);
    }
}
