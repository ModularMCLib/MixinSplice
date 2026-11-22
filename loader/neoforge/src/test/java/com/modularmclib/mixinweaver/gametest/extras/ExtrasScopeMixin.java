package com.modularmclib.mixinweaver.gametest.extras;

import com.modularmclib.mixinweaver.api.target.DynamicTargets;
import com.modularmclib.mixinweaver.gametest.target.ExtrasScopeTarget;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@DynamicTargets(relaxRequirements = true)
@Mixin(ExtrasScopeTarget.class)
abstract class ExtrasScopeMixin {

    @ModifyReturnValue(method = { "value", "onlyParent" }, at = @At("RETURN"), require = 2)
    private int adjustValue(int original) {
        return original + 10;
    }

    @Inject(method = "guard", at = @At("RETURN"), cancellable = true)
    private void guard(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(9);
    }
}
