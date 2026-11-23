package com.modularmclib.mixinsplice.api.extension;

import com.modularmclib.mixinsplice.api.condition.Condition;
import com.modularmclib.mixinsplice.api.mixin.MixinCanceller;
import com.modularmclib.mixinsplice.api.target.DynamicTargetProvider;
import com.modularmclib.mixinsplice.api.transformation.AnnotationTransformation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在 Mixin 启动阶段通过 ServiceLoader 提供扩展能力。
 * MixinSplice 按声明的顺序初始化已发现的提供者，并在运行期间复用其注册内容。
 */
public interface SpliceExtension {

    /** 全局唯一且非空的扩展 ID。重复 ID 会使注册失败。 */
    String id();

    /** 在已安装的指定扩展之前执行。缺少对应扩展时忽略该约束。 */
    default Set<String> before() {
        return Set.of();
    }

    /** 在已安装的指定扩展之后执行。缺少对应扩展时忽略该约束。 */
    default Set<String> after() {
        return Set.of();
    }

    /** 注册自定义选择条件。返回非空映射，键是全局唯一的条件 ID。 */
    default Map<String, Condition> conditions() {
        return Map.of();
    }

    /** 返回按声明顺序执行的 Mixin 取消规则。列表及元素均需非空。 */
    default List<MixinCanceller> cancellers() {
        return List.of();
    }

    /** 返回按声明顺序执行的注解调整规则。列表及元素均需非空。 */
    default List<AnnotationTransformation> transformations() {
        return List.of();
    }

    /** 注册动态目标提供者。键是全局唯一的提供者 ID，映射及其内容均需非空。 */
    default Map<String, DynamicTargetProvider> targetProviders() {
        return Map.of();
    }
}
