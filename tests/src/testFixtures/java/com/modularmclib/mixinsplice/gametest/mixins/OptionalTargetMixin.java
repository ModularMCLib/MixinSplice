package com.modularmclib.mixinsplice.gametest.mixins;

import com.modularmclib.mixinsplice.api.condition.ConditionSpec;
import com.modularmclib.mixinsplice.api.condition.Conditional;
import com.modularmclib.mixinsplice.gametest.condition.ExistingTargetCondition;
import com.modularmclib.mixinsplice.gametest.target.TransformationTarget;

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
