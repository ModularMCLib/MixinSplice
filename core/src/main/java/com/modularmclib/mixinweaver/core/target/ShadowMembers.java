package com.modularmclib.mixinweaver.core.target;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.SearchType;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Traversal;
import org.spongepowered.asm.util.Annotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 生成副本时检查声明和继承的影子成员。 */
final class ShadowMembers {

    private ShadowMembers() {}

    static Optional<String> prepare(ClassNode source, String target, Map<String, String> names) {
        Map<String, String> inherited = new HashMap<>();
        for (var field : source.fields) {
            AnnotationNode shadow = Annotations.getVisible(field, Shadow.class);
            if (shadow != null && !resolve(source, target, field.name, field.desc, shadow, false, inherited)) {
                return Optional.of("字段 @Shadow " + field.name + " 在目标上不可访问");
            }
        }
        for (var method : source.methods) {
            AnnotationNode shadow = Annotations.getVisible(method, Shadow.class);
            if (shadow != null && !resolve(source, target, method.name, method.desc, shadow, true, inherited)) {
                return Optional.of("方法 @Shadow " + method.name + method.desc + " 在目标上不可访问");
            }
        }
        source.fields.removeIf(field -> inherited.containsKey(source.name + "." + field.name));
        source.methods.removeIf(method -> inherited.containsKey(source.name + "." + method.name + method.desc));
        names.putAll(inherited);
        return Optional.empty();
    }

    private static boolean resolve(ClassNode source, String target, String name, String descriptor,
                                   AnnotationNode shadow, boolean method, Map<String, String> inherited) {
        ClassInfo owner = ClassInfo.forName(target);
        if (owner == null) {
            return false;
        }
        String prefix = Annotations.getValue(shadow, "prefix", "shadow$");
        String memberName = method && name.startsWith(prefix) ? name.substring(prefix.length()) : name;
        var remappers = MixinEnvironment.getCurrentEnvironment().getRemappers();
        String mapped = method ? remappers.mapMethodName(owner.getName(), memberName, descriptor) : remappers.mapFieldName(owner.getName(), memberName, descriptor);
        List<String> aliases = Annotations.getValue(shadow, "aliases", true);
        if (declares(owner, memberName, descriptor, method) || declares(owner, mapped, descriptor, method) || aliases.stream().anyMatch(alias -> declares(owner, alias, descriptor, method))) {
            return true;
        }

        String member = inherited(owner, mapped, descriptor, method);
        if (member == null && !mapped.equals(memberName)) {
            member = inherited(owner, memberName, descriptor, method);
        }
        if (member == null) {
            for (String alias : aliases) {
                member = inherited(owner, alias, descriptor, method);
                if (member != null) {
                    break;
                }
            }
        }
        if (member == null) {
            return false;
        }
        inherited.put(source.name + "." + name + (method ? descriptor : ""), member);
        return true;
    }

    private static boolean declares(ClassInfo owner, String name, String descriptor, boolean method) {
        return method ? owner.findMethod(name, descriptor, ClassInfo.INCLUDE_ALL) != null : owner.findField(name, descriptor, ClassInfo.INCLUDE_ALL) != null;
    }

    private static @Nullable String inherited(ClassInfo owner, String name, String descriptor, boolean method) {
        if (method) {
            var member = owner.findMethodInHierarchy(name, descriptor, SearchType.SUPER_CLASSES_ONLY, Traversal.NONE, ClassInfo.INCLUDE_STATIC);
            return member != null && accessible(owner, member.getOwner(), member.getAccess()) ? member.getName() : null;
        }
        var member = owner.findFieldInHierarchy(name, descriptor, SearchType.SUPER_CLASSES_ONLY, Traversal.NONE, ClassInfo.INCLUDE_STATIC);
        return member != null && accessible(owner, member.getOwner(), member.getAccess()) ? member.getName() : null;
    }

    private static boolean accessible(ClassInfo target, ClassInfo declaring, int access) {
        return (access & Opcodes.ACC_PRIVATE) == 0 && ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0 || packageName(target.getName()).equals(packageName(declaring.getName())));
    }

    private static String packageName(String name) {
        return name.substring(0, name.lastIndexOf('/') + 1);
    }
}
