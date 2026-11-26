package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.api.target.DynamicTargets;
import com.modularmc.mixinsplice.gametest.target.ScopeTarget.Constructed;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@DynamicTargets
@Mixin(Constructed.class)
abstract class ConstructorScopeMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void countConstruction(CallbackInfo info) {
        ((Constructed) (Object) this).increment();
    }
}
