package com.modularmclib.mixinweaver.gametest;

import net.fabricmc.api.DedicatedServerModInitializer;

public final class FabricRuntimeTestMod implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        RuntimeAssertions.verify();
    }
}
