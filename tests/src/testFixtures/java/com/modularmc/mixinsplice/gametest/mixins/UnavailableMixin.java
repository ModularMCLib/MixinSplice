package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.api.condition.ConditionSpec;
import com.modularmc.mixinsplice.api.condition.Conditional;
import com.modularmc.mixinsplice.api.mixin.CancelMixins;
import com.modularmc.mixinsplice.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec("mixinsplice_missing_gametest_mod"))
@CancelMixins("com.modularmc.mixinsplice.gametest.mixins.HandlerMixin")
@Mixin(TransformationTarget.class)
abstract class UnavailableMixin {

    @Inject(method = "unavailableValue", at = @At("RETURN"), cancellable = true)
    private void changeUnavailable(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(100);
    }
}
