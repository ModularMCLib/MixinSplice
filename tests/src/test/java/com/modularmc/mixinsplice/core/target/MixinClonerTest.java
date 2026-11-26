package com.modularmc.mixinsplice.core.target;

import com.modularmc.mixinsplice.api.target.DynamicTargets;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.util.Annotations;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MixinClonerTest {

    @Test
    void relaxesMixinExtrasAndDescriptorRequirementsOnlyOnTheCopy() {
        var source = new ClassNode();
        source.visit(Opcodes.V21, Opcodes.ACC_SUPER, "example/mixin/Source", null, "java/lang/Object", null);
        source.visitAnnotation(Type.getDescriptor(Mixin.class), false).visit("value", List.of(Type.getObjectType("example/Parent")));
        source.visitAnnotation(Type.getDescriptor(DynamicTargets.class), false);
        var handler = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_NATIVE, "handle", "()V", null, null);
        source.methods.add(handler);
        handler.visitAnnotation(Type.getDescriptor(Group.class), false).visit("min", 1);
        var extra = (AnnotationNode) handler.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;", true);
        extra.visit("method", List.of("value"));
        extra.visit("require", 1);
        var inject = (AnnotationNode) handler.visitAnnotation(Type.getDescriptor(Inject.class), true);
        var selector = new AnnotationNode(Type.getDescriptor(Desc.class));
        selector.visit("value", "other");
        selector.visit("min", 1);
        inject.visit("target", List.of(selector));
        inject.visit("require", 1);

        var generated = Objects.requireNonNull(MixinCloner.clone(source, "example.mixin.", "example.Child", true));
        var node = new ClassNode();
        new ClassReader(generated.bytes()).accept(node, 0);
        MethodNode copy = node.methods.getFirst();
        assertNull(Annotations.getInvisible(copy, Group.class));
        assertEquals(0, Annotations.<Integer>getValue(copy.visibleAnnotations.getFirst(), "require"));
        AnnotationNode clonedInject = Annotations.getVisible(copy, Inject.class);
        assertEquals(0, Annotations.<Integer>getValue(clonedInject, "require"));
        assertEquals(0, Annotations.<Integer>getValue(Annotations.<AnnotationNode>getValue(clonedInject, "target", true).getFirst(), "min"));
        assertEquals(1, Annotations.<Integer>getValue(Annotations.getInvisible(handler, Group.class), "min"));
        assertEquals(1, Annotations.<Integer>getValue(extra, "require"));
        assertEquals(1, Annotations.<Integer>getValue(selector, "min"));
    }
}
