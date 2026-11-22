package com.modularmclib.mixinweaver.api.transformation;

import java.util.List;

/**
 * 在同一 Mixin 定义第一次应用前调整类或方法的注解。
 * 同一 Mixin 的全部目标共用调整后的注解。规则按扩展顺序执行。
 */
@FunctionalInterface
public interface AnnotationTransformation {

    /**
     * 返回调整后的完整列表。空列表删除该位置的全部注解。
     * 输入为非空只读快照，返回列表与其元素均需非空。
     * 实现应为纯计算，失败时抛出异常。
     */
    List<AnnotationView> transform(AnnotationTarget target, List<AnnotationView> annotations);
}
