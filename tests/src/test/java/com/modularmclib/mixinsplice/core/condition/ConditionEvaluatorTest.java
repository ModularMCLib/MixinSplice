package com.modularmclib.mixinsplice.core.condition;

import com.modularmclib.mixinsplice.api.condition.ConditionResult;
import com.modularmclib.mixinsplice.api.condition.ConditionSpec.Kind;
import com.modularmclib.mixinsplice.api.condition.Conditional;
import com.modularmclib.mixinsplice.api.condition.UnknownPolicy;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionEvaluatorTest {

    private static final String TARGET = "example.Target";
    private static final String FIRST = "example.First";
    private static final String SECOND = "example.Second";

    @Test
    void evaluatesVersionCustomAndConflictTogether() {
        ClassNode first = mixin();
        restrict(first, UnknownPolicy.DENY, List.of(spec(Kind.MOD, "example", ">=2"),
                spec(Kind.CUSTOM, "target", "")), List.of(spec(Kind.MOD, "conflict", "")));
        ConditionEvaluator evaluator = new ConditionEvaluator(
                (mod, version) -> ConditionResult.of(mod.equals("example") && version.equals(">=2")),
                Map.of("target", context -> ConditionResult.of(context.targetClassName().equals(TARGET))),
                Map.of(FIRST, first)::get);

        assertTrue(evaluator.shouldApply(TARGET, FIRST));
        assertFalse(evaluator.shouldApply("example.Other", FIRST));
        assertFalse(evaluator.shouldApply(TARGET, SECOND));
    }

    @Test
    void checksReferencedMixinsAndTerminatesCycles() {
        ClassNode first = mixin();
        ClassNode second = mixin();
        restrict(first, UnknownPolicy.DENY, List.of(spec(Kind.MIXIN, SECOND, "")), List.of());
        ConditionEvaluator evaluator = new ConditionEvaluator(
                (mod, version) -> ConditionResult.UNKNOWN, Map.of(), Map.of(FIRST, first, SECOND, second)::get);

        assertTrue(evaluator.shouldApply(TARGET, FIRST));
        restrict(second, UnknownPolicy.DENY, List.of(spec(Kind.MIXIN, FIRST, "")), List.of());
        assertFalse(evaluator.shouldApply(TARGET, FIRST));
    }

    @Test
    void handlesUnknownAndDecisiveConflicts() {
        ClassNode first = mixin();
        ConditionEvaluator evaluator = new ConditionEvaluator(
                (mod, version) -> mod.equals("known") ? ConditionResult.TRUE : ConditionResult.UNKNOWN,
                Map.of(), Map.of(FIRST, first)::get);
        List<AnnotationNode> unknown = List.of(spec(Kind.MOD, "unknown", ""));

        restrict(first, UnknownPolicy.DENY, unknown, List.of());
        assertFalse(evaluator.shouldApply(TARGET, FIRST));
        restrict(first, UnknownPolicy.ALLOW, unknown, List.of());
        assertTrue(evaluator.shouldApply(TARGET, FIRST));
        restrict(first, UnknownPolicy.FAIL, unknown, List.of());
        assertThrows(IllegalStateException.class, () -> evaluator.shouldApply(TARGET, FIRST));
        restrict(first, UnknownPolicy.FAIL, unknown, List.of(spec(Kind.MOD, "known", "")));
        assertFalse(evaluator.shouldApply(TARGET, FIRST));
    }

    private static ClassNode mixin() {
        ClassNode node = new ClassNode();
        node.visitAnnotation(Type.getDescriptor(Mixin.class), false).visitEnd();
        return node;
    }

    private static AnnotationNode spec(Kind kind, String value, String version) {
        AnnotationNode node = new AnnotationNode("Lcom/modularmclib/mixinsplice/api/condition/ConditionSpec;");
        node.visitEnum("kind", Type.getDescriptor(Kind.class), kind.name());
        node.visit("value", value);
        node.visit("version", version);
        return node;
    }

    private static void restrict(ClassNode node, UnknownPolicy policy, List<AnnotationNode> require, List<AnnotationNode> conflict) {
        node.invisibleAnnotations.removeIf(annotation -> annotation.desc.equals(Type.getDescriptor(Conditional.class)));
        var annotation = node.visitAnnotation(Type.getDescriptor(Conditional.class), false);
        annotation.visitEnum("unknown", Type.getDescriptor(UnknownPolicy.class), policy.name());
        var requires = annotation.visitArray("require");
        require.forEach(spec -> spec.accept(requires.visitAnnotation(null, spec.desc)));
        requires.visitEnd();
        var conflicts = annotation.visitArray("conflict");
        conflict.forEach(spec -> spec.accept(conflicts.visitAnnotation(null, spec.desc)));
        conflicts.visitEnd();
        annotation.visitEnd();
    }
}
