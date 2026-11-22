package com.modularmclib.mixinweaver.core.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Mixin 0.8.7 的内部访问边界。
 * ITargetClassContext 公开目标字节码，IMixinInfo#getClassNode 返回副本。
 * 取消应用和调整原始注解分别需要当前目标的可变 Mixin 集合与定义状态。
 */
final class MixinInternals {

    private MixinInternals() {}

    private static final class Application {

        private static final Class<?> CONTEXT = type("TargetClassContext");
        private static final Class<?> INFO = type("MixinInfo");
        private static final Field ACTIVE_MIXINS = field(CONTEXT, "mixins", Collection.class);
        private static final Field STATE = field(INFO, "state", type("MixinInfo$State"));
        private static final Field CLASS_NODE = field(STATE.getType(), "classNode", ClassNode.class);
    }

    static void verifyApplicationAccess() {
        if (Application.CLASS_NODE.getType() != ClassNode.class) {
            throw new IllegalStateException("当前 Mixin 运行时的定义状态与 Mixin 0.8.7 不同");
        }
    }

    static Collection<?> activeMixins(ITargetClassContext context) {
        return (Collection<?>) read(Application.ACTIVE_MIXINS, context);
    }

    static ClassNode originalBytecode(IMixinInfo mixin) {
        return (ClassNode) read(Application.CLASS_NODE, read(Application.STATE, mixin));
    }

    private static Class<?> type(String simpleName) {
        try {
            return ClassInfo.class.getClassLoader().loadClass(ClassInfo.class.getPackageName() + "." + simpleName);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("当前 Mixin 运行时缺少内部类型 " + simpleName, exception);
        }
    }

    private static Field field(Class<?> owner, String name, Class<?> type) {
        try {
            Field field = owner.getDeclaredField(name);
            if (!type.isAssignableFrom(field.getType()) || !field.trySetAccessible()) {
                throw new IllegalStateException("当前 Mixin 运行时无法访问内部字段 " + owner.getName() + "." + name);
            }
            return field;
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("当前 Mixin 运行时缺少内部字段 " + owner.getName() + "." + name, exception);
        }
    }

    private static Object read(Field field, Object owner) {
        try {
            Object value = field.get(owner);
            if (value == null) {
                throw new IllegalStateException("Mixin 状态尚未初始化 " + field);
            }
            return value;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法读取 Mixin 状态 " + field, exception);
        }
    }
}
