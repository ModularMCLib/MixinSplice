package com.modularmclib.mixinweaver.core.target;

import com.modularmclib.mixinweaver.api.target.DynamicTargets;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.util.Annotations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MixinCloner {

    private MixinCloner() {}

    public static GeneratedMixin clone(ClassNode source, String mixinPackage, String target, boolean relax) {
        String name = source.name + "$MixinWeaver$" + UUID.nameUUIDFromBytes((source.name + "#" + target).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        ClassNode clone = new ClassNode();
        source.accept(new ClassRemapper(clone, new SimpleRemapper(source.name, name)));
        if ((clone.access & Opcodes.ACC_INTERFACE) != 0) {
            convertInterface(clone);
        }
        AnnotationNode mixin = Annotations.getInvisible(clone, Mixin.class);
        if (mixin == null) {
            throw new IllegalArgumentException("动态目标缺少 @Mixin " + source.name);
        }
        mixin.values = new ArrayList<>(mixin.values);
        for (int i = mixin.values.size() - 2; i >= 0; i -= 2) {
            Object key = mixin.values.get(i);
            if ("targets".equals(key) || "value".equals(key)) {
                mixin.values.subList(i, i + 2).clear();
            }
        }
        mixin.visit("value", List.of(Type.getObjectType(target.replace('.', '/'))));
        clone.invisibleAnnotations.removeIf(annotation -> annotation.desc.equals(Type.getDescriptor(DynamicTargets.class)));
        if (relax) {
            relaxRequirements(clone);
        }
        var writer = new ClassWriter(0);
        clone.accept(writer);
        String binaryName = name.replace('/', '.');
        return new GeneratedMixin(source.name.replace('/', '.'), binaryName,
                binaryName.substring(mixinPackage.length()), writer.toByteArray());
    }

    private static void convertInterface(ClassNode clone) {
        clone.access = (clone.access & ~Opcodes.ACC_INTERFACE) | Opcodes.ACC_SUPER | Opcodes.ACC_ABSTRACT;
        for (var method : clone.methods) {
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(clone.name) && call.itf) {
                    call.itf = false;
                    if (call.getOpcode() == Opcodes.INVOKEINTERFACE) {
                        call.setOpcode(Opcodes.INVOKEVIRTUAL);
                    }
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    for (int i = 0; i < dynamic.bsmArgs.length; i++) {
                        if (dynamic.bsmArgs[i] instanceof Handle handle && handle.isInterface() && handle.getOwner().equals(clone.name)) {
                            int tag = handle.getTag() == Opcodes.H_INVOKEINTERFACE ? Opcodes.H_INVOKEVIRTUAL : handle.getTag();
                            dynamic.bsmArgs[i] = new Handle(tag, handle.getOwner(), handle.getName(), handle.getDesc(), false);
                        }
                    }
                }
            }
        }
    }

    private static void relaxRequirements(ClassNode node) {
        for (var method : node.methods) {
            if (method.visibleAnnotations == null) {
                continue;
            }
            method.visibleAnnotations.removeIf(annotation -> Type.getDescriptor(Group.class).equals(annotation.desc));
            for (AnnotationNode annotation : method.visibleAnnotations) {
                if (annotation.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/") && Annotations.getValue(annotation, "method") != null) {
                    Annotations.setValue(annotation, "require", 0);
                    Annotations.setValue(annotation, "expect", 0);
                }
            }
        }
    }
}
