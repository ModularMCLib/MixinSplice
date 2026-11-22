package com.modularmclib.mixinweaver.api.condition;

/**
 * 提供选择阶段可获取的环境信息。调用方仅在 Condition 求值期间使用该对象。
 * 所有字符串使用非空值，输入与返回的类名均使用点分隔。
 */
public interface ConditionContext {

    /** 当前被检查的 Mixin 二进制类名。 */
    String mixinClassName();

    /** 本次选择对应的目标类二进制类名。 */
    String targetClassName();

    /** 判断 Mod 是否存在并满足 Loader 的版本谓词。空谓词只检查存在性。 */
    ConditionResult mod(String modId, String versionPredicate);

    /** 判断另一个 Mixin 的类和限制条件是否满足。循环依赖返回 UNKNOWN。 */
    ConditionResult mixin(String mixinClassName);
}
