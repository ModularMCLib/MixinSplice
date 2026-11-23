package com.modularmclib.mixinsplice.api.target;

import java.util.Collection;

/**
 * 在准备阶段为一个 Mixin 选择额外目标。
 * 实现只查询字节码和类层次，避免加载待转换的类。上下文仅在当次调用期间有效。
 */
@FunctionalInterface
public interface DynamicTargetProvider {

    /** 返回点分隔的目标类名集合。集合及其元素均需非空。 */
    Collection<String> select(TargetContext context);
}
