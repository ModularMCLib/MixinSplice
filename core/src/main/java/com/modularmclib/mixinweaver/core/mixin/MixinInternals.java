package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.core.target.GeneratedMixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.service.IMixinService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final class Preparation {

        private static final Class<?> CONFIG = type("MixinConfig");
        private static final Field MIXIN_MAPPING = field(CONFIG, "mixinMapping", Map.class);
        private static final Field PENDING_MIXINS = field(ClassInfo.class, "mixins", Set.class);
        private static final Field REFMAP = field(CONFIG, "refMapper", IReferenceMapper.class);
        private static final Field SERVICE = field(CONFIG, "service", IMixinService.class);
        private static final Method PREPARE = method(CONFIG, "prepareMixins", String.class, List.class, boolean.class, Extensions.class);
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

    // getAppliedMixins 只返回已应用的 Mixin。准备阶段先读取已发现的声明。
    static List<IMixinInfo> preparedMixins(Set<String> targets) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var mixins = new ArrayList<IMixinInfo>();
        for (String target : targets) {
            ClassInfo info = ClassInfo.fromCache(target);
            if (info != null) {
                for (Object mixin : (Set<?>) read(Preparation.PENDING_MIXINS, info)) {
                    if (seen.add(mixin)) {
                        mixins.add(IMixinInfo.class.cast(mixin));
                    }
                }
            }
        }
        mixins.sort(Comparator.comparing((IMixinInfo mixin) -> mixin.getConfig().getName()).thenComparing(IMixinInfo::getClassName));
        return mixins;
    }

    static IReferenceMapper referenceMapper(IMixinConfig config) {
        return (IReferenceMapper) read(Preparation.REFMAP, config);
    }

    static void removeTarget(IMixinInfo mixin, String target) {
        Map<?, ?> mapping = (Map<?, ?>) read(Preparation.MIXIN_MAPPING, mixin.getConfig());
        if (mapping.get(target) instanceof Collection<?> mixins) {
            mixins.remove(mixin);
        }
    }

    // 配置插件只能提供类名，生成的 Mixin 需要在准备阶段提供内存字节码。
    static void prepareGenerated(IMixinConfig config, List<GeneratedMixin> generated) {
        if (generated.isEmpty()) {
            return;
        }
        GeneratedRefmap refmap = GeneratedRefmap.forConfig(config, referenceMapper(config));
        refmap.add(generated);
        write(Preparation.REFMAP, config, refmap);

        IMixinService base = (IMixinService) read(Preparation.SERVICE, config);
        write(Preparation.SERVICE, config, GeneratedBytecodeService.wrap(base, generated));
        try {
            Preparation.PREPARE.invoke(config, "MixinWeaver 动态目标",
                    generated.stream().map(GeneratedMixin::relativeName).toList(), true, MixinWeaverExtension.extensions());
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("注册动态目标失败 " + config.getName(), exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法调用 Mixin 0.8.7 的准备入口 " + config.getName(), exception);
        } finally {
            write(Preparation.SERVICE, config, base);
        }
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

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            if (!method.trySetAccessible()) {
                throw new IllegalStateException("当前 Mixin 运行时无法调用 " + owner.getName() + "." + name);
            }
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("当前 Mixin 运行时缺少内部方法 " + owner.getName() + "." + name, exception);
        }
    }

    private static void write(Field field, Object owner, Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法更新 Mixin 状态 " + field, exception);
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
