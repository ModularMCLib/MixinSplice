package com.modularmclib.mixinweaver.gametest;

import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("mixinweaver")
@PrefixGameTestTemplate(false)
public final class TransformationGameTest {

    @GameTest(template = "empty")
    public static void appliesOnlySelectedMixinsAndAdjustsHandlers(GameTestHelper helper) {
        var target = new TransformationTarget();
        helper.assertTrue(target.handlerValue() == 42, "跨 Mixin 注入应替换处理方法的结果");
        helper.assertTrue(target.adjustedValue() == 99, "注解调整应改变注入目标");
        helper.assertTrue(target.cancelledValue() == 7, "已取消的 Mixin 不应运行");
        helper.assertTrue(target.unavailableValue() == 8, "缺少依赖时条件 Mixin 不应运行");
        helper.succeed();
    }
}
