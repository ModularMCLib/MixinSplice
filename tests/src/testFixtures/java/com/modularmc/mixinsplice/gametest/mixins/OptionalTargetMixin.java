package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.api.condition.ConditionSpec;
import com.modularmc.mixinsplice.api.condition.Conditional;
import com.modularmc.mixinsplice.gametest.condition.ExistingTargetCondition;
import com.modularmc.mixinsplice.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec(tester = ExistingTargetCondition.class))
@Mixin(value = TransformationTarget.class, targets = "example.missing.ConditionalTarget", remap = false)
abstract class OptionalTargetMixin {

    @Inject(method = "optionalValue", at = @At("RETURN"), cancellable = true)
    private void updateOptionalValue(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(17);
    }
}
