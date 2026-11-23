package com.modularmclib.mixinsplice.gametest.plugin;

import com.modularmclib.mixinsplice.gametest.target.PluginTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PluginTarget.class)
abstract class CancelledPluginMixin {

    @Inject(method = "cancelled", at = @At("RETURN"), cancellable = true)
    private void replaceCancelled(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(100);
    }
}
