package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.condition.ConditionSpec;
import com.modularmclib.mixinweaver.api.condition.ConditionSpec.Kind;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec(kind = Kind.MOD, value = "neoforge", version = ">=21.1"))
@Mixin(value = TransformationTarget.class, priority = 1100)
abstract class OriginMixin {

    @Inject(method = "handlerValue", at = @At("RETURN"), cancellable = true)
    private void changeHandler(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(10);
    }

    @Inject(method = "missingUntilAdjusted", at = @At("RETURN"), cancellable = true)
    private void changeAdjusted(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(99);
    }
}
