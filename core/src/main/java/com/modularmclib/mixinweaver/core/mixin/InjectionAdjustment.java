package com.modularmclib.mixinweaver.core.mixin;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;

record InjectionAdjustment(String source, String mixin, String handler, List<String> selectors) {

    InjectionAdjustment {
        if (mixin.isBlank() || handler.isBlank() || selectors.isEmpty() || selectors.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("@AdjustInjection 需要 Mixin、处理方法和新的注入目标 " + source);
        }
        selectors = List.copyOf(selectors);
    }

    void apply(IMixinInfo info, ClassNode bytecode, List<Runnable> restore) {
        List<MethodNode> matching = bytecode.methods.stream()
                .filter(method -> handler.equals(method.name) || handler.equals(method.name + method.desc)).toList();
        if (matching.size() != 1) {
            throw invalid("需要定位到唯一的处理方法，找到 " + matching.size() + " 个");
        }
        MethodNode method = matching.getFirst();
        AnnotationNode injection = InjectionInfo.getInjectorAnnotation(info, method);
        if (injection == null) {
            throw invalid("处理方法缺少注入器");
        }

        var replacement = new AnnotationNode(injection.desc);
        injection.accept(replacement);
        Annotations.setValue(replacement, "method", selectors);
        Annotations.setValue(replacement, "target", List.of());
        List<AnnotationNode> original = method.visibleAnnotations;
        var annotations = new ArrayList<>(original);
        annotations.set(annotations.indexOf(injection), replacement);
        restore.add(() -> method.visibleAnnotations = original);
        method.visibleAnnotations = annotations;
    }

    private IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("@AdjustInjection " + source + " -> " + mixin + "#" + handler + "：" + reason);
    }
}
