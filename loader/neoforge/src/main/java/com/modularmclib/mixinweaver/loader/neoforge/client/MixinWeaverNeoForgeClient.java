package com.modularmclib.mixinweaver.loader.neoforge.client;

import com.modularmclib.mixinweaver.loader.neoforge.MixinWeaverNeoForge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

@Mod(value = MixinWeaverNeoForge.MOD_ID, dist = Dist.CLIENT)
public class MixinWeaverNeoForgeClient {

    public MixinWeaverNeoForgeClient(IEventBus modBus, FMLModContainer container) {
        ClientProxy.init(modBus);
    }
}
