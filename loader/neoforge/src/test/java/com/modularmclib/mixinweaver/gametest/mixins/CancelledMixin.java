package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransformationTarget.class)
abstract class CancelledMixin {

    @Inject(method = "cancelledValue", at = @At("RETURN"), cancellable = true)
    private void changeCancelled(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(100);
    }
}
