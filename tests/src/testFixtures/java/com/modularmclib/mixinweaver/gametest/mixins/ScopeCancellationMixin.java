package com.modularmclib.mixinweaver.gametest.mixins;

import com.modularmclib.mixinweaver.api.mixin.CancelMixins;
import com.modularmclib.mixinweaver.gametest.target.ScopeTarget.Cancelled;

import org.spongepowered.asm.mixin.Mixin;

@CancelMixins("com.modularmclib.mixinweaver.gametest.mixins.ScopeMixin")
@Mixin(Cancelled.class)
abstract class ScopeCancellationMixin {}
