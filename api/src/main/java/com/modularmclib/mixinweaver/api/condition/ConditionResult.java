package com.modularmclib.mixinweaver.api.condition;

/** 条件结果，环境尚未提供足够信息时返回 UNKNOWN。 */
public enum ConditionResult {

    TRUE,
    FALSE,
    UNKNOWN;

    public static ConditionResult of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public ConditionResult not() {
        return switch (this) {
            case TRUE -> FALSE;
            case FALSE -> TRUE;
            case UNKNOWN -> UNKNOWN;
        };
    }
}
