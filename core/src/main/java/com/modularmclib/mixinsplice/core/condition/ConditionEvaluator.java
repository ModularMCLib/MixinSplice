package com.modularmclib.mixinsplice.core.condition;

import com.modularmclib.mixinsplice.api.condition.Condition;
import com.modularmclib.mixinsplice.api.condition.ConditionContext;
import com.modularmclib.mixinsplice.api.condition.ConditionQueries.Decision;
import com.modularmclib.mixinsplice.api.condition.ConditionResult;
import com.modularmclib.mixinsplice.api.condition.ConditionSpec;
import com.modularmclib.mixinsplice.api.condition.Conditional;
import com.modularmclib.mixinsplice.api.condition.Conditions;
import com.modularmclib.mixinsplice.api.condition.UnknownPolicy;
import com.modularmclib.mixinsplice.core.platform.PlatformEnvironment;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ConditionEvaluator {

    private final PlatformEnvironment platform;
    private final Map<String, Condition> conditions;
    private final Function<String, @Nullable ClassNode> bytecode;
    private final Map<Type, Condition> testers = new ConcurrentHashMap<>();

    public ConditionEvaluator(PlatformEnvironment platform, Map<String, Condition> conditions,
                              Function<String, @Nullable ClassNode> bytecode) {
        this.platform = platform;
        this.conditions = Map.copyOf(conditions);
        this.bytecode = bytecode;
    }

    public boolean shouldApply(String target, String mixin) {
        return check(target, mixin).allowed();
    }

    public Decision check(String target, String mixin) {
        return check(target, mixin, bytecode.apply(mixin));
    }

    public Decision check(String target, String mixin, @Nullable ClassNode node) {
        Evaluation evaluation = evaluate(target, mixin, new LinkedHashSet<>(), node);
        return new Decision(evaluation.result == ConditionResult.TRUE, evaluation.reason);
    }

    private Evaluation evaluate(String target, String mixin, Set<String> checking, @Nullable ClassNode node) {
        if (!checking.add(mixin)) {
            return new Evaluation(ConditionResult.UNKNOWN, "条件依赖形成循环 " + checking);
        }
        try {
            if (node == null || Annotations.getInvisible(node, Mixin.class) == null) {
                return new Evaluation(ConditionResult.FALSE, "找不到 Mixin " + mixin);
            }
            AnnotationNode annotation = Annotations.getInvisible(node, Conditional.class);
            if (annotation == null) {
                return new Evaluation(ConditionResult.TRUE, "");
            }
            return evaluateRestrictions(annotation, new Context(target, mixin, checking));
        } finally {
            checking.remove(mixin);
        }
    }

    private Evaluation evaluateRestrictions(AnnotationNode annotation, Context context) {
        List<Rule> required = rules(annotation, "require");
        List<Rule> conflicts = rules(annotation, "conflict");
        Evaluation requirements = checkRules(required, context, ConditionResult.TRUE, "未满足必要条件 ");
        if (requirements.result == ConditionResult.FALSE) {
            return requirements;
        }
        Evaluation exclusions = checkRules(conflicts, context, ConditionResult.FALSE, "命中冲突条件 ");
        if (exclusions.result == ConditionResult.FALSE) {
            return exclusions;
        }
        Evaluation uncertain = requirements.result == ConditionResult.UNKNOWN ? requirements : exclusions;
        if (uncertain.result != ConditionResult.UNKNOWN) {
            return uncertain;
        }
        UnknownPolicy policy = Annotations.getValue(annotation, "unknown", UnknownPolicy.class, UnknownPolicy.DENY);
        return switch (policy) {
            case DENY -> new Evaluation(ConditionResult.FALSE, uncertain.reason);
            case ALLOW -> new Evaluation(ConditionResult.TRUE, "");
            case FAIL -> throw new IllegalStateException("无法判断 " + context.mixinClassName + " 对 " + context.targetClassName + " 的选择条件：" + uncertain.reason);
        };
    }

    private static Evaluation checkRules(List<Rule> rules, Context context, ConditionResult expected, String rejection) {
        Evaluation evaluation = new Evaluation(ConditionResult.TRUE, "");
        for (Rule rule : rules) {
            ConditionResult result = Objects.requireNonNull(rule.condition.evaluate(context), rule.description);
            if (result == ConditionResult.UNKNOWN) {
                if (evaluation.result != ConditionResult.UNKNOWN) {
                    evaluation = new Evaluation(ConditionResult.UNKNOWN, "无法判断条件 " + rule.description);
                }
            } else if (result != expected) {
                return new Evaluation(ConditionResult.FALSE, rejection + rule.description);
            }
        }
        return evaluation;
    }

    private List<Rule> rules(AnnotationNode annotation, String category) {
        List<AnnotationNode> entries = Annotations.getValue(annotation, category, true);
        return entries.stream().map(this::parseRule).toList();
    }

    private Rule parseRule(AnnotationNode spec) {
        ConditionSpec.Kind kind = Annotations.getValue(spec, "kind", ConditionSpec.Kind.class, ConditionSpec.Kind.MOD);
        String value = Annotations.getValue(spec, "value", "");
        String version = Annotations.getValue(spec, "version", "");
        List<String> versions = Annotations.getValue(spec, "versions", true);
        Type tester = Annotations.getValue(spec, "tester", Type.getType(Condition.class));
        if (!tester.equals(Type.getType(Condition.class))) {
            if (!value.isEmpty() || !version.isEmpty() || !versions.isEmpty() || kind == ConditionSpec.Kind.MIXIN) {
                throw new IllegalArgumentException("条件类不能与 Mod、Mixin 或注册 ID 同时使用 " + tester.getClassName());
            }
            return new Rule(context -> testers.computeIfAbsent(tester, ConditionEvaluator::loadTester).evaluate(context), tester.getClassName());
        }
        if (value.isBlank() || (kind != ConditionSpec.Kind.MOD && (!version.isEmpty() || !versions.isEmpty())) || (!version.isEmpty() && !versions.isEmpty()) || versions.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("无效的 MixinSplice 条件 " + value);
        }
        Condition condition = switch (kind) {
            case MOD -> versions.isEmpty() ? Conditions.modVersion(value, version) : Conditions.anyOf(versions.stream().map(predicate -> Conditions.modVersion(value, predicate)).toArray(Condition[]::new));
            case MIXIN -> Conditions.mixinPresent(value);
            case CUSTOM -> Objects.requireNonNull(conditions.get(value), "未注册的 MixinSplice 条件 " + value);
        };
        String description = switch (kind) {
            case MOD -> "Mod " + value + (versions.isEmpty() ? (version.isEmpty() ? "" : " " + version) : " " + versions);
            case MIXIN -> "Mixin " + value;
            case CUSTOM -> "自定义条件 " + value;
        };
        return new Rule(condition, description);
    }

    private static Condition loadTester(Type tester) {
        // 注解中的类值来自字节码。由 Mixin 的类提供器解析扩展点，再实例化公开构造器。
        try {
            return MixinService.getService().getClassProvider().findClass(tester.getClassName(), true)
                    .asSubclass(Condition.class).getConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("无法创建条件类 " + tester.getClassName(), exception);
        }
    }

    private record Rule(Condition condition, String description) {}

    private record Evaluation(ConditionResult result, String reason) {}

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
            return checking.contains(className) ? ConditionResult.UNKNOWN : evaluate(targetClassName, className, checking, bytecode.apply(className)).result;
        }
    }
}
