package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.api.target.DynamicTargets;
import com.modularmc.mixinsplice.gametest.target.ScopeTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@DynamicTargets(relaxRequirements = true)
@Mixin(ScopeTarget.class)
abstract class ScopeMixin {

    @Shadow
    protected int offset;

    @Shadow
    protected abstract int shadow$additionalOffset();

    @Inject(method = "value", at = @At("RETURN"), cancellable = true)
    private void modifyValue(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(info.getReturnValueI() + offset + shadow$additionalOffset());
    }

    @Group(name = "requiredExtra", min = 1)
    @Inject(target = @Desc(value = "extra", ret = int.class, min = 1), at = @At("RETURN"), cancellable = true, require = 1)
    private void modifyExtra(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(9);
    }
}
