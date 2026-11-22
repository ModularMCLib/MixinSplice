package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.api.mixin.CancelMixins;
import com.modularmclib.mixinweaver.api.target.DynamicTargets;
import com.modularmclib.mixinweaver.api.transformation.AdjustInjection;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class MixinDeclarations {

    private static final Set<String> METADATA = Set.of(Type.getDescriptor(Conditional.class),
            Type.getDescriptor(DynamicTargets.class), Type.getDescriptor(CancelMixins.class),
            Type.getDescriptor(AdjustInjection.class), Type.getDescriptor(AdjustInjection.List.class));

    private MixinDeclarations() {}

    static List<String> cancellations(ClassNode bytecode) {
        AnnotationNode annotation = Annotations.getInvisible(bytecode, CancelMixins.class);
        List<String> names = Annotations.getValue(annotation, "value", true);
        if (names.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("@CancelMixins 需要有效的 Mixin 类名 " + bytecode.name);
        }
        return names;
    }

    static List<InjectionAdjustment> adjustments(ClassNode bytecode) {
        AnnotationNode single = Annotations.getInvisible(bytecode, AdjustInjection.class);
        AnnotationNode repeated = Annotations.getInvisible(bytecode, AdjustInjection.List.class);
        if (single != null) {
            return List.of(readAdjustment(bytecode.name, single));
        }
        return Annotations.<AnnotationNode>getValue(repeated, "value", true).stream()
                .map(annotation -> readAdjustment(bytecode.name, annotation)).toList();
    }

    private static InjectionAdjustment readAdjustment(String source, AnnotationNode annotation) {
        return new InjectionAdjustment(source, Annotations.getValue(annotation, "mixin", ""),
                Annotations.getValue(annotation, "handler", ""), Annotations.getValue(annotation, "method", true));
    }

    static void hideMetadata(ClassNode bytecode, List<Runnable> restore) {
        List<AnnotationNode> original = bytecode.invisibleAnnotations;
        if (original != null && original.stream().anyMatch(annotation -> METADATA.contains(annotation.desc))) {
            var remaining = new ArrayList<>(original);
            remaining.removeIf(annotation -> METADATA.contains(annotation.desc));
            restore.add(() -> bytecode.invisibleAnnotations = original);
            bytecode.invisibleAnnotations = remaining;
        }
    }
}
