package com.modularmclib.mixinsplice.core.handler;

import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.asm.util.logging.MessageRouter;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;

import java.lang.reflect.Field;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

/** 复用 Mixin 注解处理器的映射服务及正在输出的 Refmap。 */
final class HandlerCompilation {

    private static boolean registered;

    private HandlerCompilation() {}

    static synchronized void register() {
        if (!registered) {
            // Mixin 的默认日志依赖游戏服务。注解处理器初始化后会设置实际的编译器诊断入口。
            MessageRouter.setMessager(new CompilerMessager());
            TargetSelector.register(HandlerSelector.class, "mixinsplice");
            registered = true;
        }
    }

    static void addMapping(ISelectorContext selector, String owner, String reference, MemberInfo member) {
        if (member.getDesc() == null || selector.getAnnotation() == null || !selector.getAnnotation().getValue("remap", true)) {
            return;
        }
        IMixinContext context = selector.getMixin();
        var manager = read(context, "obf", IObfuscationManager.class);
        var mapping = new MappingMethod(owner.replace('.', '/'), member.getName(), member.getDesc());
        // Mod 自己的方法通常没有方法映射，描述符里的游戏类型仍需重映射。
        var obfuscated = manager.getDataProvider().getRemappedMethod(mapping);
        if (!obfuscated.isEmpty()) {
            manager.getReferenceManager().addMethodMapping(context.getClassRef(), reference, obfuscated);
        }
    }

    private static <T> T read(IMixinContext context, String name, Class<T> type) {
        // IMixinContext 公开 Refmap 读取，编译期映射写入器由注解处理器持有。
        try {
            Field field = context.getClass().getDeclaredField(name);
            if (!field.trySetAccessible()) {
                throw new IllegalStateException("无法访问 Mixin 注解处理器字段 " + name);
            }
            return type.cast(field.get(context));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Mixin 注解处理器缺少字段 " + name, exception);
        }
    }

    private static final class CompilerMessager implements Messager {

        @Override
        public void printMessage(Kind kind, CharSequence message) {
            if (kind == Kind.ERROR || kind == Kind.WARNING || kind == Kind.MANDATORY_WARNING) {
                System.err.println(message);
            }
        }

        @Override
        public void printMessage(Kind kind, CharSequence message, Element element) {
            printMessage(kind, message);
        }

        @Override
        public void printMessage(Kind kind, CharSequence message, Element element, AnnotationMirror annotation) {
            printMessage(kind, message);
        }

        @Override
        public void printMessage(Kind kind, CharSequence message, Element element, AnnotationMirror annotation, AnnotationValue value) {
            printMessage(kind, message);
        }
    }
}
