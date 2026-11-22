package com.modularmclib.mixinweaver.api.condition;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * 提供 Mixin 配置插件和其他调用方主动查询条件的入口。
 * Loader 开始发现 Mod 后可调用。查询不会加载目标类或改变 Mixin 的选择状态。
 */
public interface ConditionQueries {

    /** 返回当前 Loader 的查询入口。发行包缺失时抛出异常。 */
    static ConditionQueries get() {
        return ServiceLoader.load(ConditionQueries.class, ConditionQueries.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到 MixinWeaver 的条件查询入口"));
    }

    /** 以二进制类名指定目标和 Mixin。无效配置抛出异常。 */
    Decision check(String target, String mixin);

    /** 条件拒绝时同步报告原因，然后返回 false。 */
    default boolean shouldApply(String target, String mixin, Consumer<String> onRejected) {
        Decision decision = check(target, mixin);
        if (!decision.allowed()) {
            onRejected.accept(decision.reason());
        }
        return decision.allowed();
    }

    /** Mod 不存在或 Loader 尚未提供 Mod 列表时返回空。 */
    Optional<String> modVersion(String modId);

    /** 按当前 Loader 的语法匹配给定版本。无效的版本谓词抛出异常。 */
    boolean matchesVersion(String version, String predicate);

    /** 通过时 reason 为空字符串。拒绝时包含此次判断的原因。 */
    record Decision(boolean allowed, String reason) {}
}
