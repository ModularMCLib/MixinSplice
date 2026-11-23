package com.modularmclib.mixinsplice.gametest.forge;

import com.modularmclib.mixinsplice.gametest.RuntimeAssertions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.server.Main", remap = false)
abstract class ForgeMainMixin {

    // 通过服务端的真实类加载入口执行测试，--help 会在 Minecraft 初始化前退出。
    @Inject(method = "main", at = @At("HEAD"))
    private static void verifyMixins(String[] args, CallbackInfo info) {
        RuntimeAssertions.verify();
    }
}
