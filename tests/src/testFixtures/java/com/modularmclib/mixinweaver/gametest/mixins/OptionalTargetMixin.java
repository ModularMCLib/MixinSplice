package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.condition.ConditionSpec;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.gametest.condition.ExistingTargetCondition;
import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

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
