package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.api.handler.TargetHandler;
import com.modularmc.mixinsplice.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransformationTarget.class)
abstract class HandlerMixin {

    @TargetHandler(mixin = "mixins.com.modularmc.mixinsplice.gametest.OriginMixin",
                   config = "mixinsplice.gametest.mixins.json",
                   prefix = "handler",
                   name = "changeHandler(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V")
    @Inject(method = "@mixinsplice:handler", at = @At("RETURN"))
    private void afterHandler(CallbackInfoReturnable<Integer> result, CallbackInfo info) {
        result.setReturnValue(42);
    }

    @TargetHandler(mixin = "mixins.com.modularmc.mixinsplice.gametest.OriginMixin", name = "changeHandler", print = true)
    @Inject(method = "@mixinsplice:handler", at = @At("RETURN"), require = 0, expect = 0)
    private void printCandidates(CallbackInfoReturnable<Integer> result, CallbackInfo info) {
        throw new AssertionError("输出候选时不应注入");
    }
}
