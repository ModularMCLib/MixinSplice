package com.modularmclib.mixinsplice.gametest.mixins;

import com.modularmclib.mixinsplice.api.mixin.CancelMixins;
import com.modularmclib.mixinsplice.gametest.target.ScopeTarget.Cancelled;

import org.spongepowered.asm.mixin.Mixin;

@CancelMixins("com.modularmclib.mixinsplice.gametest.mixins.ScopeMixin")
@Mixin(Cancelled.class)
abstract class ScopeCancellationMixin {}
