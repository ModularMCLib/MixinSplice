package com.modularmc.mixinsplice.gametest.mixins;

import com.modularmc.mixinsplice.api.mixin.CancelMixins;
import com.modularmc.mixinsplice.gametest.target.ScopeTarget.Cancelled;

import org.spongepowered.asm.mixin.Mixin;

@CancelMixins("mixins.com.modularmc.mixinsplice.gametest.ScopeMixin")
@Mixin(Cancelled.class)
abstract class ScopeCancellationMixin {}
