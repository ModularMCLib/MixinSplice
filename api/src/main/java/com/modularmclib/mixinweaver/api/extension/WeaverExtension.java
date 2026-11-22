package com.modularmclib.mixinweaver.api.extension;

import com.modularmclib.mixinweaver.api.condition.Condition;

import java.util.Map;
import java.util.Set;

/**
 * 在 Mixin 启动阶段通过 ServiceLoader 提供扩展能力。
 * MixinWeaver 按声明的顺序初始化已发现的提供者，并在运行期间复用其注册内容。
 */
public interface WeaverExtension {

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
}
