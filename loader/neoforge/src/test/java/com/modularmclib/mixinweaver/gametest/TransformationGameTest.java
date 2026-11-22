package com.modularmclib.mixinweaver.gametest;

import com.modularmclib.mixinweaver.gametest.target.ScopeTarget;
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

    @GameTest(template = "empty")
    public static void expandsBypassingOverridesAndInterfaceTargets(GameTestHelper helper) {
        helper.assertTrue(new ScopeTarget().value(false) == 11, "声明目标应保留原有注入");
        helper.assertTrue(new ScopeTarget.Inherited().value(false) == 11, "只继承父实现的类应运行一次注入");
        helper.assertTrue(new ScopeTarget.Delegating().value(false) == 11, "完全委托父实现的类应运行一次注入");
        helper.assertTrue(new ScopeTarget.Bypassing().value(false) == 12, "绕过父实现的覆盖者应获得动态注入");
        helper.assertTrue(new ScopeTarget.Partial().value(true) == 13, "条件绕过父实现的路径应获得动态注入");
        helper.assertTrue(new ScopeTarget.Bypassing().extra() == 9, "生成目标可缺少声明目标专有的注入点");
        helper.assertTrue(new ScopeTarget.Implementation().score() == 14, "接口实现类应获得动态注入");
        helper.succeed();
    }
}
