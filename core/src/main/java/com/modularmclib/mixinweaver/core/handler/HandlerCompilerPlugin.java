package com.modularmclib.mixinweaver.core.handler;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

/** 在 javac 开始运行 Mixin 的注解处理器前注册 Handler。 */
public final class HandlerCompilerPlugin implements Plugin {

    @Override
    public String getName() {
        return "MixinWeaver";
    }

    @Override
    public boolean autoStart() {
        return true;
    }

    @Override
    public void init(JavacTask task, String... arguments) {
        try {
            getClass().getClassLoader().loadClass("org.spongepowered.tools.obfuscation.MixinObfuscationProcessorInjection");
        } catch (ClassNotFoundException absent) {
            // 普通编译也可能看见发行包。只有存在 Mixin 注解处理器时才注册。
            return;
        }
        HandlerCompilation.register();
    }
}
