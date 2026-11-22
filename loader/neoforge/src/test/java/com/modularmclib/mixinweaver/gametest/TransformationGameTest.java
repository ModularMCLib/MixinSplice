package com.modularmclib.mixinweaver.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("mixinweaver")
@PrefixGameTestTemplate(false)
public final class TransformationGameTest {

    @GameTest(template = "empty")
    public static void appliesOnlySelectedMixinsAndAdjustsHandlers(GameTestHelper helper) {
        RuntimeAssertions.appliesOnlySelectedMixinsAndAdjustsHandlers();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void expandsBypassingOverridesAndInterfaceTargets(GameTestHelper helper) {
        RuntimeAssertions.expandsBypassingOverridesAndInterfaceTargets();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void selectsAndExpandsPluginMixins(GameTestHelper helper) {
        RuntimeAssertions.selectsAndExpandsPluginMixins();
        helper.succeed();
    }
}
