package com.modularmc.mixinsplice.api.condition;

import java.util.List;

/** 构造可组合的选择条件。 */
public final class Conditions {

    private Conditions() {}

    public static Condition modLoaded(String modId) {
        return modVersion(modId, "");
    }

    public static Condition modVersion(String modId, String versionPredicate) {
        return context -> context.mod(modId, versionPredicate);
    }

    public static Condition mixinPresent(String className) {
        return context -> context.mixin(className);
    }

    public static Condition not(Condition condition) {
        return context -> condition.evaluate(context).not();
    }

    public static Condition allOf(Condition... conditions) {
        List<Condition> children = List.of(conditions);
        return context -> {
            ConditionResult result = ConditionResult.TRUE;
            for (Condition child : children) {
                ConditionResult value = child.evaluate(context);
                if (value == ConditionResult.FALSE) {
                    return value;
                }
                if (value == ConditionResult.UNKNOWN) {
                    result = value;
                }
            }
            return result;
        };
    }

    public static Condition anyOf(Condition... conditions) {
        List<Condition> children = List.of(conditions);
        return context -> {
            ConditionResult result = ConditionResult.FALSE;
            for (Condition child : children) {
                ConditionResult value = child.evaluate(context);
                if (value == ConditionResult.TRUE) {
                    return value;
                }
                if (value == ConditionResult.UNKNOWN) {
                    result = value;
                }
            }
            return result;
        };
    }
}
