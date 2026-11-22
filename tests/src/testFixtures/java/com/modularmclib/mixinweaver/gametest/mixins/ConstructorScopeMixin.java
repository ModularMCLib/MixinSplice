package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.target.DynamicTargets;
import com.modularmclib.mixinweaver.gametest.target.ScopeTarget.Constructed;

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
