package com.modularmc.mixinsplice.core.platform;

import com.modularmc.mixinsplice.api.condition.ConditionResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

    /** 返回 Loader 报告的版本。Mod 不可见时返回空。 */
    default Optional<String> modVersion(String modId) {
        return Optional.empty();
    }

    /** 在任意版本上使用当前 Loader 的谓词规则。 */
    default boolean matchesVersion(String version, String predicate) {
        throw new UnsupportedOperationException("当前平台未提供版本解析");
    }

    /** 返回当前阶段可见的类路径根。仅在存在动态目标时扫描这些路径。 */
    default List<Path> classpathRoots() {
        return List.of();
    }
}
