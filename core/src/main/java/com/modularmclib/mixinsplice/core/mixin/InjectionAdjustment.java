package com.modularmclib.mixinsplice.core.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.List;

record InjectionAdjustment(String source, String mixin, String handler, AnnotationNode declaration) {

    private static final List<String> MEMBERS = List.of("method", "target", "at", "slice", "ordinals", "require", "expect", "allow");

    InjectionAdjustment {
        if (mixin.isBlank() || handler.isBlank()) {
            throw new IllegalArgumentException("@AdjustInjection 需要 Mixin 和处理方法 " + source);
        }
        boolean remove = Boolean.TRUE.equals(Annotations.getValue(declaration, "remove"));
        boolean hasMembers = MEMBERS.stream().anyMatch(name -> Annotations.getValue(declaration, name) != null);
        if (remove == hasMembers) {
            throw new IllegalArgumentException("@AdjustInjection 需要指定要修改的属性或 remove = true " + source);
        }
        if (Annotations.<String>getValue(declaration, "method", true).stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("@AdjustInjection 的方法选择器不能为空 " + source);
        }
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

        List<AnnotationNode> original = method.visibleAnnotations;
        var annotations = new ArrayList<>(original);
        if (Boolean.TRUE.equals(Annotations.getValue(declaration, "remove"))) {
            annotations.remove(injection);
        } else {
            var replacement = new AnnotationNode(injection.desc);
            injection.accept(replacement);
            update(replacement);
            annotations.set(annotations.indexOf(injection), replacement);
        }
        restore.add(() -> method.visibleAnnotations = original);
        method.visibleAnnotations = annotations;
    }

    private void update(AnnotationNode injection) {
        Object methods = Annotations.getValue(declaration, "method");
        List<AnnotationNode> targets = Annotations.getValue(declaration, "target");
        if (methods != null || targets != null) {
            Annotations.setValue(injection, "method", methods == null ? List.of() : methods);
            Annotations.setValue(injection, "target", targets == null ? List.of() : copyAnnotations(targets));
        }
        replaceAnnotations(injection, "at");
        replaceAnnotations(injection, "slice");
        List<AnnotationNode> points = Annotations.getValue(injection, "at", true);
        for (AnnotationNode ordinal : Annotations.<AnnotationNode>getValue(declaration, "ordinals", true)) {
            int index = Annotations.getValue(ordinal, "index", 0);
            int value = Annotations.getValue(ordinal, "value", -2);
            if (index < 0 || index >= points.size() || value < -1) {
                throw invalid("无效的注入点位置 " + index + " 或序号 " + value);
            }
            Annotations.setValue(points.get(index), "ordinal", value);
        }
        for (String name : List.of("require", "expect", "allow")) {
            Integer count = Annotations.getValue(declaration, name);
            if (count != null) {
                Annotations.setValue(injection, name, count);
            }
        }
    }

    private void replaceAnnotations(AnnotationNode injection, String member) {
        List<AnnotationNode> declared = Annotations.getValue(declaration, member);
        if (declared == null) {
            return;
        }
        Object current = Annotations.getValue(injection, member);
        boolean array = current == null ? arrayMember(injection, member) : current instanceof List<?>;
        if (!array && declared.size() != 1) {
            throw invalid("属性 " + member + " 只接受一个注解");
        }
        var copies = copyAnnotations(declared);
        Annotations.setValue(injection, member, array ? copies : copies.getFirst());
    }

    private static List<AnnotationNode> copyAnnotations(List<AnnotationNode> annotations) {
        return annotations.stream().map(annotation -> {
            var copy = new AnnotationNode(annotation.desc);
            annotation.accept(copy);
            return copy;
        }).toList();
    }

    private boolean arrayMember(AnnotationNode injection, String member) {
        String name = Type.getType(injection.desc).getClassName();
        @Nullable
        ClassNode type = MixinBytecode.metadata(name);
        if (type != null) {
            for (MethodNode method : type.methods) {
                if (method.name.equals(member)) {
                    return Type.getReturnType(method.desc).getSort() == Type.ARRAY;
                }
            }
        }
        throw invalid(name + " 缺少属性 " + member);
    }

    private IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("@AdjustInjection " + source + " -> " + mixin + "#" + handler + "：" + reason);
    }
}
