package com.modularmclib.mixinsplice.gametest.condition;

import com.modularmclib.mixinsplice.api.condition.Condition;
import com.modularmclib.mixinsplice.api.condition.ConditionContext;
import com.modularmclib.mixinsplice.api.condition.ConditionResult;

public final class ExistingTargetCondition implements Condition {

    @Override
    public ConditionResult evaluate(ConditionContext context) {
        return ConditionResult.of(context.targetClassName().endsWith(".TransformationTarget"));
    }
}
