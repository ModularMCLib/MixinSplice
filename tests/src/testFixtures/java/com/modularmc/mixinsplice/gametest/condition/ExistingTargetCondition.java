package com.modularmc.mixinsplice.gametest.condition;

import com.modularmc.mixinsplice.api.condition.Condition;
import com.modularmc.mixinsplice.api.condition.ConditionContext;
import com.modularmc.mixinsplice.api.condition.ConditionResult;

public final class ExistingTargetCondition implements Condition {

    @Override
    public ConditionResult evaluate(ConditionContext context) {
        return ConditionResult.of(context.targetClassName().endsWith(".TransformationTarget"));
    }
}
