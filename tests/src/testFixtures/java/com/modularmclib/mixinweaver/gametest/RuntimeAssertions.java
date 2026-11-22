package com.modularmclib.mixinweaver.gametest;

import com.modularmclib.mixinweaver.gametest.target.PluginTarget;
import com.modularmclib.mixinweaver.gametest.target.ScopeTarget;
import com.modularmclib.mixinweaver.gametest.target.TransformationTarget;

public final class RuntimeAssertions {

    private RuntimeAssertions() {}

    public static void appliesOnlySelectedMixinsAndAdjustsHandlers() {
        var target = new TransformationTarget();
        assertTrue(target.handlerValue() == 42, "跨 Mixin 注入应替换处理方法的结果");
        assertTrue(target.adjustedValue() == 99, "注解调整应改变注入目标");
        assertTrue(target.originalValue() == 6, "替换选择器后应跳过原注入目标");
        assertTrue(target.cancelledValue() == 7, "已取消的 Mixin 不应运行");
        assertTrue(target.unavailableValue() == 8, "缺少依赖时条件 Mixin 不应运行");
        var other = new TransformationTarget.Other();
        assertTrue(other.adjustedValue() == 5 && other.originalValue() == 99, "其他目标应保留原注入选择器");
        assertTrue(other.cancelledValue() == 100, "其他目标应继续应用同一个 Mixin");
    }

    public static void expandsBypassingOverridesAndInterfaceTargets() {
        assertTrue(new ScopeTarget().value(false) == 11, "声明目标应保留原有注入");
        assertTrue(new ScopeTarget.Inherited().value(false) == 11, "只继承父实现的类应运行一次注入");
        assertTrue(new ScopeTarget.Delegating().value(false) == 11, "完全委托父实现的类应运行一次注入");
        var bypassing = new ScopeTarget.Bypassing();
        assertTrue(bypassing.value(false) == 32, "动态目标中的处理方法应可通过原始 Mixin 名称定位");
        assertTrue(new ScopeTarget.Partial().value(true) == 13, "条件绕过父实现的路径应获得动态注入");
        assertTrue(bypassing.extra() == 9, "生成目标仍应继承声明目标的注入结果");
        assertTrue(bypassing.alternate() == 9, "注解调整应能通过原始 Mixin 名称匹配动态副本");
        assertTrue(new ScopeTarget.Cancelled().value(false) == 5, "取消规则应能通过原始 Mixin 名称匹配动态副本");
        assertTrue(new ScopeTarget.Implementation().score() == 14, "接口实现类应获得动态注入");
    }

    public static void selectsAndExpandsPluginMixins() {
        var target = new PluginTarget();
        assertTrue(target.amount() == 20, "插件应能追加 Mixin");
        assertTrue(target.unavailable() == 3, "插件追加的 Mixin 应受条件选择");
        assertTrue(target.cancelled() == 4, "程序化取消应跳过被取消的 Mixin");
        assertTrue(target.original() == 5, "程序化注解变换应替换注入选择器");
        assertTrue(new PluginTarget.Child().amount() == 20, "插件追加的 Mixin 应生成动态目标");
    }

    public static void verify() {
        appliesOnlySelectedMixinsAndAdjustsHandlers();
        expandsBypassingOverridesAndInterfaceTargets();
        selectsAndExpandsPluginMixins();
        System.out.println("MixinWeaver runtime tests passed");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
