package com.modularmclib.mixinweaver.api.condition;

/** 当已知条件不足以决定是否应用 Mixin 时采用的策略。 */
public enum UnknownPolicy {
    DENY,
    ALLOW,
    FAIL
}
