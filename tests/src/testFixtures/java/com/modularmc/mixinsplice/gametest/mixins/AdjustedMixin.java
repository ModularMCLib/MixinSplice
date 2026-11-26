package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.gametest.target.TransformationTarget;
import com.modularmc.mixinsplice.gametest.target.TransformationTarget.Other;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ TransformationTarget.class, Other.class })
abstract class AdjustedMixin {

    @Inject(method = "ordinalValue", at = @At("RETURN"), cancellable = true)
    private void changeOrdinal(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(97);
    }

    @Inject(method = "originalValue", at = @At("RETURN"), cancellable = true)
    private void changeAdjusted(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(99);
    }
}
