package com.modularmclib.mixinweaver.loader.neoforge.bootstrap;

import net.neoforged.bus.api.IEventBus;

public class CommonProxy {

    private static IEventBus modBus;

    public static void init(final IEventBus modBus) {
        CommonProxy.modBus = modBus;
    }
}
