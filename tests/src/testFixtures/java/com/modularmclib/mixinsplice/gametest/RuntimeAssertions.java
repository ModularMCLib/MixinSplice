package com.modularmclib.mixinsplice.gametest;

import com.modularmclib.mixinsplice.api.condition.ConditionQueries;
import com.modularmclib.mixinsplice.gametest.target.PluginTarget;
import com.modularmclib.mixinsplice.gametest.target.ScopeTarget;
import com.modularmclib.mixinsplice.gametest.target.TransformationTarget;

public final class RuntimeAssertions {

    private RuntimeAssertions() {}

    public static void appliesOnlySelectedMixinsAndAdjustsHandlers() {
        var target = new TransformationTarget();
        assertTrue(target.handlerValue() == 42, "跨 Mixin 注入应替换处理方法的结果");
        assertTrue(target.adjustedValue() == 99, "注解调整应改变注入目标");
        assertTrue(target.ordinalValue(true) == 3 && target.ordinalValue(false) == 97, "序号调整只应选中第二个返回位置");
        assertTrue(target.originalValue() == 6, "替换选择器后应跳过原注入目标");
        assertTrue(target.cancelledValue() == 7, "已取消的 Mixin 不应运行");
        assertTrue(target.unavailableValue() == 8, "缺少依赖时条件 Mixin 不应运行");
        assertTrue(target.optionalValue() == 17, "类条件应保留存在的目标");
        var queries = ConditionQueries.get();
        String version = queries.modVersion("minecraft").orElseThrow();
        assertTrue(queries.matchesVersion(version, "1.21.x"), "应能使用当前平台的版本谓词查询");
        String mixin = "com.modularmclib.mixinsplice.gametest.mixins.OptionalTargetMixin";
        assertTrue(queries.check(TransformationTarget.class.getName(), mixin).allowed(), "主动查询应允许存在的目标");
        assertTrue(!queries.shouldApply("example.missing.ConditionalTarget", mixin,
                reason -> assertTrue(reason.contains("ExistingTargetCondition"), "应报告拒绝目标的条件")), "主动查询应拒绝缺失的目标");
        var other = new TransformationTarget.Other();
        assertTrue(other.adjustedValue() == 5 && other.originalValue() == 99, "其他目标应保留原注入选择器");
        assertTrue(other.ordinalValue(true) == 97 && other.ordinalValue(false) == 97, "其他目标应保留原注入点范围");
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
        assertTrue(new ScopeTarget.DefaultDelegating().score() == 14, "委托接口默认方法的实现类也应获得动态注入");
        assertTrue(new ScopeTarget.Constructed().count() == 1, "声明类的构造器应应用注入");
        assertTrue(new ScopeTarget.ConstructedChild().count() == 2, "子类构造器应有自己的动态注入");
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
        System.out.println("MixinSplice runtime tests passed");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
