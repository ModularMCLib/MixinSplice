package com.modularmclib.mixinweaver.gametest.plugin;

import com.modularmclib.mixinweaver.api.condition.ConditionSpec;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.gametest.target.PluginTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec("mixinweaver_missing_plugin_mod"))
@Mixin(PluginTarget.class)
abstract class UnavailablePluginMixin {

    @Inject(method = "unavailable", at = @At("RETURN"), cancellable = true)
    private void replaceAmount(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(100);
    }
}
