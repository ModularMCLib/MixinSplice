package com.modularmclib.mixinweaver.core.condition;

import com.modularmclib.mixinweaver.api.condition.Condition;
import com.modularmclib.mixinweaver.api.condition.ConditionContext;
import com.modularmclib.mixinweaver.api.condition.ConditionResult;
import com.modularmclib.mixinweaver.api.condition.ConditionSpec;
import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.api.condition.Conditions;
import com.modularmclib.mixinweaver.api.condition.UnknownPolicy;
import com.modularmclib.mixinweaver.core.platform.PlatformEnvironment;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.util.Annotations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class ConditionEvaluator {

    private final PlatformEnvironment platform;
    private final Map<String, Condition> conditions;
    private final Function<String, @Nullable ClassNode> bytecode;

    public ConditionEvaluator(PlatformEnvironment platform, Map<String, Condition> conditions,
                              Function<String, @Nullable ClassNode> bytecode) {
        this.platform = platform;
        this.conditions = Map.copyOf(conditions);
        this.bytecode = bytecode;
    }

    public boolean shouldApply(String target, String mixin) {
        return evaluate(target, mixin, new LinkedHashSet<>()) == ConditionResult.TRUE;
    }

    private ConditionResult evaluate(String target, String mixin, Set<String> checking) {
        if (!checking.add(mixin)) {
            return ConditionResult.UNKNOWN;
        }
        try {
            ClassNode node = bytecode.apply(mixin);
            if (node == null || Annotations.getInvisible(node, Mixin.class) == null) {
                return ConditionResult.FALSE;
            }
            AnnotationNode annotation = Annotations.getInvisible(node, Conditional.class);
            if (annotation == null) {
                return ConditionResult.TRUE;
            }
            return evaluateRestrictions(annotation, new Context(target, mixin, checking));
        } finally {
            checking.remove(mixin);
        }
    }

    private ConditionResult evaluateRestrictions(AnnotationNode annotation, Context context) {
        Condition required = Conditions.allOf(rules(annotation, "require"));
        Condition conflicts = Conditions.anyOf(rules(annotation, "conflict"));
        ConditionResult result = Conditions.allOf(required, Conditions.not(conflicts)).evaluate(context);
        if (result != ConditionResult.UNKNOWN) {
            return result;
        }
        UnknownPolicy policy = Annotations.getValue(annotation, "unknown", UnknownPolicy.class, UnknownPolicy.DENY);
        return switch (policy) {
            case DENY -> ConditionResult.FALSE;
            case ALLOW -> ConditionResult.TRUE;
            case FAIL -> throw new IllegalStateException("无法判断 " + context.mixinClassName + " 对 " + context.targetClassName + " 的选择条件，可能存在循环条件 " + context.checking);
        };
    }

    private Condition[] rules(AnnotationNode annotation, String category) {
        List<AnnotationNode> entries = Annotations.getValue(annotation, category, true);
        return entries.stream().map(this::parseRule).toArray(Condition[]::new);
    }

    private Condition parseRule(AnnotationNode spec) {
        ConditionSpec.Kind kind = Annotations.getValue(spec, "kind", ConditionSpec.Kind.class, ConditionSpec.Kind.MOD);
        String value = Annotations.getValue(spec, "value", "");
        String version = Annotations.getValue(spec, "version", "");
        if (value.isBlank() || (kind != ConditionSpec.Kind.MOD && !version.isEmpty())) {
            throw new IllegalArgumentException("无效的 MixinWeaver 条件 " + value);
        }
        return switch (kind) {
            case MOD -> Conditions.modVersion(value, version);
            case MIXIN -> Conditions.mixinPresent(value);
            case CUSTOM -> Objects.requireNonNull(conditions.get(value), "未注册的 MixinWeaver 条件 " + value);
        };
    }

    private final class Context implements ConditionContext {

        private final String targetClassName;
        private final String mixinClassName;
        private final Set<String> checking;

        private Context(String targetClassName, String mixinClassName, Set<String> checking) {
            this.targetClassName = targetClassName;
            this.mixinClassName = mixinClassName;
            this.checking = checking;
        }

        @Override
        public String mixinClassName() {
            return mixinClassName;
        }

        @Override
        public String targetClassName() {
            return targetClassName;
        }

        @Override
        public ConditionResult mod(String modId, String versionPredicate) {
            return platform.mod(modId, versionPredicate);
        }

        @Override
        public ConditionResult mixin(String className) {
            return evaluate(targetClassName, className, checking);
        }
    }
}
