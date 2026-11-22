package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.target.DynamicTargets;
import com.modularmclib.mixinweaver.gametest.target.ScopeTarget.Contract;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@DynamicTargets
@Mixin(Contract.class)
interface InterfaceScopeMixin {

    @Inject(method = "score", at = @At("RETURN"), cancellable = true)
    private void modifyScore(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(14);
    }
}
