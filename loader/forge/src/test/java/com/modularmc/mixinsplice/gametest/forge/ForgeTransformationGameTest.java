package com.modularmc.mixinsplice.gametest.forge;

import com.modularmclib.mixinsplice.gametest.RuntimeAssertions;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder("mixinsplice")
public final class ForgeTransformationGameTest {

    @GameTest(template = "mixinsplice:empty")
    public static void verifiesRuntimeTransformations(GameTestHelper helper) {
        RuntimeAssertions.verify();
        helper.succeed();
    }
}
