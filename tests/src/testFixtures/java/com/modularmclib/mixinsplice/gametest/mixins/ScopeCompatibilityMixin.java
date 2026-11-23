package com.modularmclib.mixinsplice.gametest.mixins;

import com.modularmclib.mixinsplice.api.handler.TargetHandler;
import com.modularmclib.mixinsplice.api.transformation.AdjustInjection;
import com.modularmclib.mixinsplice.gametest.target.ScopeTarget.Bypassing;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@AdjustInjection(mixin = "com.modularmclib.mixinsplice.gametest.mixins.ScopeMixin", handler = "modifyExtra", method = "alternate")
@Mixin(Bypassing.class)
abstract class ScopeCompatibilityMixin {

    @TargetHandler(mixin = "com.modularmclib.mixinsplice.gametest.mixins.ScopeMixin", name = "modifyValue")
    @Inject(method = "@mixinsplice:handler", at = @At("RETURN"))
    private void afterHandler(CallbackInfoReturnable<Integer> result, CallbackInfo info) {
        result.setReturnValue(result.getReturnValueI() + 20);
    }
}
