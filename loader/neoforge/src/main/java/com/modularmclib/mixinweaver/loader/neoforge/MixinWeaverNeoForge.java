package com.modularmclib.mixinweaver.loader.neoforge;

import com.modularmclib.mixinweaver.loader.neoforge.bootstrap.CommonProxy;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

@Mod(MixinWeaverNeoForge.MOD_ID)
public class MixinWeaverNeoForge {

    public static final String MOD_ID = "mixinweaver";
    public static final String MOD_NAME = "MixinWeaver";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @ApiStatus.Internal
    public static IEventBus tenModBus;

    public MixinWeaverNeoForge(IEventBus modBus, FMLModContainer container) {
        MixinWeaverNeoForge.tenModBus = modBus;
        CommonProxy.init(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
