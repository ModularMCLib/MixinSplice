package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;
import com.modularmclib.mixinweaver.gametest.target.TransformationTarget.Other;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ TransformationTarget.class, Other.class })
abstract class AdjustedMixin {

    @Inject(method = "originalValue", at = @At("RETURN"), cancellable = true)
    private void changeAdjusted(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(99);
    }
}
