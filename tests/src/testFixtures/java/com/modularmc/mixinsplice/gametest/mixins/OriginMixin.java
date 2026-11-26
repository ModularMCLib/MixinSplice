package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.api.condition.ConditionSpec;
import com.modularmc.mixinsplice.api.condition.Conditional;
import com.modularmc.mixinsplice.api.mixin.CancelMixins;
import com.modularmc.mixinsplice.api.transformation.AdjustInjection;
import com.modularmc.mixinsplice.api.transformation.AdjustInjection.AtOrdinal;
import com.modularmc.mixinsplice.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec(value = "minecraft", versions = { ">=1.22", "1.21.x" }))
@CancelMixins("com.modularmc.mixinsplice.gametest.mixins.CancelledMixin")
@AdjustInjection(mixin = "com.modularmc.mixinsplice.gametest.mixins.AdjustedMixin", handler = "changeAdjusted", method = "originalValue")
@AdjustInjection(mixin = "com.modularmc.mixinsplice.gametest.mixins.AdjustedMixin",
                 handler = "changeAdjusted",
                 target = @Desc(value = "adjustedValue", ret = int.class))
@AdjustInjection(mixin = "com.modularmc.mixinsplice.gametest.mixins.AdjustedMixin",
                 handler = "changeOrdinal",
                 at = @At("RETURN"),
                 slice = @Slice(from = @At("HEAD"), to = @At("TAIL")),
                 ordinals = @AtOrdinal(1),
                 require = 1)
@Mixin(value = TransformationTarget.class, priority = 1100)
abstract class OriginMixin {

    @Inject(method = "handlerValue", at = @At("RETURN"), cancellable = true)
    private void changeHandler(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(10);
    }
}
