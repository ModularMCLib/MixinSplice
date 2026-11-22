package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.condition.ConditionSpec;
import com.modularmclib.mixinweaver.api.condition.ConditionSpec.Kind;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec(kind = Kind.MOD, value = "mixinweaver_missing_gametest_mod"))
@Mixin(TransformationTarget.class)
abstract class UnavailableMixin {

    @Inject(method = "unavailableValue", at = @At("RETURN"), cancellable = true)
    private void changeUnavailable(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(100);
    }
}
