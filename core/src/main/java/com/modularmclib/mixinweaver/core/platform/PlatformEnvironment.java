package com.modularmclib.mixinweaver.core.platform;

import com.modularmclib.mixinweaver.api.condition.ConditionResult;

/**
 * 为 Core 提供 Loader 发现阶段的环境快照。
 * 平台适配器由 ServiceLoader 提供，调用可能早于 Mod 入口构造。
 */
@FunctionalInterface
public interface PlatformEnvironment {

    /**
     * 使用非空 Mod ID 和版本谓词判断条件。空谓词只检查是否存在。
     * 平台尚未完成发现时返回 UNKNOWN，版本谓词无效时抛出异常。
     */
    ConditionResult mod(String modId, String versionPredicate);
}
