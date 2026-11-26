package com.modularmc.mixinsplice.core.target;

import com.modularmc.mixinsplice.api.target.DynamicTargets;
import com.modularmc.mixinsplice.core.mixin.InjectionAnnotations;

import org.jspecify.annotations.Nullable;
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
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class MixinCloner {

    private MixinCloner() {}

    public static boolean isAccessorOnly(ClassNode source) {
        if ((source.access & Opcodes.ACC_INTERFACE) == 0) {
            return false;
        }
        var methods = source.methods.stream().filter(method -> (method.access & Opcodes.ACC_SYNTHETIC) == 0).toList();
        return !methods.isEmpty() && methods.stream().allMatch(method -> Annotations.getVisible(method, Accessor.class) != null || Annotations.getVisible(method, Invoker.class) != null);
    }

    public static @Nullable GeneratedMixin clone(ClassNode source, String mixinPackage, String target, boolean relax) {
        String name = source.name + "$MixinSplice$" + UUID.nameUUIDFromBytes((source.name + "#" + target).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        var mappings = new HashMap<String, String>();
        mappings.put(source.name, name);
        var copy = new ClassNode();
        source.accept(copy);
        var unavailable = ShadowMembers.prepare(copy, target, mappings);
        if (unavailable.isPresent()) {
            MixinService.getService().getLogger("mixinsplice").warn("跳过 {} -> {}：{}", source.name, target, unavailable.get());
            return null;
        }
        ClassNode clone = new ClassNode();
        // ASM 9.9 起才支持传入 API 版本，早期加载器使用旧构造函数。
        @SuppressWarnings({ "deprecation", "RedundantSuppression" })
        var remapper = new SimpleRemapper(mappings);
        copy.accept(new ClassRemapper(clone, remapper));
        if (clone.innerClasses != null) {
            clone.innerClasses.stream().filter(inner -> inner.outerName == null && inner.name.startsWith(source.name + "$"))
                    .forEach(inner -> inner.outerName = name);
        }
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
            InjectionAnnotations.relax(clone);
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
}
