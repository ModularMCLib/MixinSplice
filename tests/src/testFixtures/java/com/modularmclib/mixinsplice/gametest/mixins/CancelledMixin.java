package com.modularmclib.mixinsplice.gametest.mixins;

import com.modularmclib.mixinsplice.gametest.target.TransformationTarget;
import com.modularmclib.mixinsplice.gametest.target.TransformationTarget.Other;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ TransformationTarget.class, Other.class })
abstract class CancelledMixin {

    @Inject(method = "cancelledValue", at = @At("RETURN"), cancellable = true)
    private void changeCancelled(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(100);
    }
}
