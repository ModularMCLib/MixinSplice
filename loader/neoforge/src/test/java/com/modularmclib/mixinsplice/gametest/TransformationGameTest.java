package com.modularmclib.mixinsplice.gametest;

import com.modularmclib.mixinsplice.gametest.target.ExtrasScopeTarget;
import com.modularmclib.mixinsplice.gametest.target.ExtrasScopeTarget.Child;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("mixinsplice")
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

    @GameTest(template = "empty")
    public static void expandsMixinExtrasWithMissingInjectionPoints(GameTestHelper helper) {
        var parent = new ExtrasScopeTarget();
        var child = new Child();
        helper.assertTrue(parent.value() == 12 && parent.onlyParent() == 13 && parent.guard() == 9, "声明类应应用两种注入器");
        helper.assertTrue(child.value() == 15 && child.onlyParent() == 13, "副本应在只有一个匹配注入点时成功应用");
        helper.succeed();
    }
}
