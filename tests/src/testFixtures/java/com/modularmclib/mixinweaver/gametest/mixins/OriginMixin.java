package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.condition.ConditionSpec;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.api.mixin.CancelMixins;
import com.modularmclib.mixinweaver.api.transformation.AdjustInjection;
import com.modularmclib.mixinweaver.api.transformation.AdjustInjection.AtOrdinal;
import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Conditional(require = @ConditionSpec(value = "minecraft", versions = { ">=1.22", "1.21.x" }))
@CancelMixins("com.modularmclib.mixinweaver.gametest.mixins.CancelledMixin")
@AdjustInjection(mixin = "com.modularmclib.mixinweaver.gametest.mixins.AdjustedMixin", handler = "changeAdjusted", method = "originalValue")
@AdjustInjection(mixin = "com.modularmclib.mixinweaver.gametest.mixins.AdjustedMixin",
                 handler = "changeAdjusted",
                 target = @Desc(value = "adjustedValue", ret = int.class))
@AdjustInjection(mixin = "com.modularmclib.mixinweaver.gametest.mixins.AdjustedMixin",
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
