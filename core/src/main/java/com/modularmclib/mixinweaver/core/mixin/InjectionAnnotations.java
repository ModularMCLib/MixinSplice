package com.modularmclib.mixinweaver.core.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.util.Annotations;

import java.util.List;
import java.util.stream.Stream;

/** 供目标分析和副本生成共用的注入器注解识别。 */
public final class InjectionAnnotations {

    private static final String GROUP = Type.getDescriptor(Group.class);
    private static final String OVERWRITE = Type.getDescriptor(Overwrite.class);

    private InjectionAnnotations() {}

    public static boolean hasInjectionOrOverwrite(ClassNode source) {
        return source.methods.stream().flatMap(InjectionAnnotations::on).anyMatch(
                annotation -> isInjector(annotation) || OVERWRITE.equals(annotation.desc));
    }

    public static Stream<String> selectors(ClassNode source) {
        return source.methods.stream().flatMap(method -> on(method).flatMap(annotation -> {
            if (OVERWRITE.equals(annotation.desc)) {
                return Stream.of(method.name + method.desc);
            }
            if (!isInjector(annotation)) {
                return Stream.empty();
            }
            List<String> methods = Annotations.getValue(annotation, "method", true);
            List<AnnotationNode> targets = Annotations.getValue(annotation, "target", true);
            return Stream.concat(methods.stream(), targets.stream().map(InjectionAnnotations::descriptor));
        }));
    }

    private static String descriptor(AnnotationNode target) {
        Type result = Annotations.getValue(target, "ret", Type.VOID_TYPE);
        List<Type> arguments = Annotations.getValue(target, "args", true);
        String name = Annotations.getValue(target, "value", "");
        return name.isBlank() ? "" : name + Type.getMethodDescriptor(result, arguments.toArray(Type[]::new));
    }

    public static void relax(ClassNode source) {
        for (MethodNode method : source.methods) {
            relax(method.visibleAnnotations);
            relax(method.invisibleAnnotations);
        }
    }

    private static void relax(@Nullable List<AnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }
        // Mixin 按组至少要求一次成功，而且拒绝 min = 0。可选副本需移除分组校验。
        annotations.removeIf(annotation -> GROUP.equals(annotation.desc));
        for (AnnotationNode annotation : annotations) {
            if (isInjector(annotation)) {
                Annotations.setValue(annotation, "require", 0);
                Annotations.setValue(annotation, "expect", 0);
                for (AnnotationNode target : Annotations.<AnnotationNode>getValue(annotation, "target", true)) {
                    Annotations.setValue(target, "min", 0);
                }
            }
        }
    }

    private static Stream<AnnotationNode> on(MethodNode method) {
        return Stream.concat(stream(method.visibleAnnotations), stream(method.invisibleAnnotations));
    }

    private static Stream<AnnotationNode> stream(@Nullable List<AnnotationNode> annotations) {
        return annotations == null ? Stream.empty() : annotations.stream();
    }

    private static boolean isInjector(AnnotationNode annotation) {
        return (annotation.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/") || annotation.desc.startsWith("Lcom/llamalad7/mixinextras/injector/")) && (Annotations.getValue(annotation, "method") != null || Annotations.getValue(annotation, "target") != null);
    }
}
