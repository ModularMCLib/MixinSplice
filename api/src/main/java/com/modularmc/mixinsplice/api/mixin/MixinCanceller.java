package com.modularmc.mixinsplice.api.mixin;

/**
 * 为目标类取消一个已发现的 Mixin。MixinSplice 在选择或实际应用前调用此规则。
 * 实现应保持无副作用，允许同一 Mixin 的不同目标得到不同结果。
 */
@FunctionalInterface
public interface MixinCanceller {

    /**
     * 返回 true 以取消该 Mixin 对指定目标的应用。
     * 两个参数均非空。运行时异常会中断当前转换并保留异常原因。
     */
    boolean shouldCancel(MixinSubject mixin, String targetClassName);
}
