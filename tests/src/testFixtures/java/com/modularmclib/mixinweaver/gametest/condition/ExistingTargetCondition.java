package com.modularmclib.mixinweaver.gametest.condition;

import com.modularmclib.mixinweaver.api.condition.Condition;
import com.modularmclib.mixinweaver.api.condition.ConditionContext;
import com.modularmclib.mixinweaver.api.condition.ConditionResult;

public final class ExistingTargetCondition implements Condition {

    @Override
    public ConditionResult evaluate(ConditionContext context) {
        return ConditionResult.of(context.targetClassName().endsWith(".TransformationTarget"));
    }
}
