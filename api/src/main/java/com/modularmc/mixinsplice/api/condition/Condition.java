package com.modularmc.mixinsplice.api.condition;

/**
 * 在 Mixin 选择阶段计算应用条件。
 * 实现应无副作用，且允许 Mixin 针对不同目标重复求值。
 */
@FunctionalInterface
public interface Condition {

    /**
     * 使用当次选择上下文计算结果。环境信息不可用时返回 UNKNOWN。
     * 上下文和结果均不能为空，配置错误应抛出异常。
     */
    ConditionResult evaluate(ConditionContext context);
}
