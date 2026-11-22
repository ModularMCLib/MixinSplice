package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.handler.TargetHandler;
import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransformationTarget.class)
abstract class HandlerMixin {

    @TargetHandler(mixin = "com.modularmclib.mixinweaver.gametest.mixins.OriginMixin",
                   config = "mixinweaver.gametest.mixins.json",
                   prefix = "handler",
                   name = "changeHandler(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V")
    @Inject(method = "@mixinweaver:handler", at = @At("RETURN"))
    private void afterHandler(CallbackInfoReturnable<Integer> result, CallbackInfo info) {
        result.setReturnValue(42);
    }

    @TargetHandler(mixin = "com.modularmclib.mixinweaver.gametest.mixins.OriginMixin", name = "changeHandler", print = true)
    @Inject(method = "@mixinweaver:handler", at = @At("RETURN"), require = 0, expect = 0)
    private void printCandidates(CallbackInfoReturnable<Integer> result, CallbackInfo info) {
        throw new AssertionError("输出候选时不应注入");
    }
}
