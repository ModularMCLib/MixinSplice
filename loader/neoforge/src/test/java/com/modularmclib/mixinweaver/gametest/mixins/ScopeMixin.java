package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.target.DynamicTargets;
import com.modularmclib.mixinweaver.gametest.target.ScopeTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@DynamicTargets(relaxRequirements = true)
@Mixin(ScopeTarget.class)
abstract class ScopeMixin {

    @Inject(method = "value", at = @At("RETURN"), cancellable = true)
    private void modifyValue(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(info.getReturnValueI() + 10);
    }

    @Inject(method = "extra", at = @At("RETURN"), cancellable = true, require = 1)
    private void modifyExtra(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(9);
    }
}
