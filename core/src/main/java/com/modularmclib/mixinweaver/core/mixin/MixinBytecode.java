package com.modularmclib.mixinweaver.core.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;

public final class MixinBytecode {

    private MixinBytecode() {}

    public static @Nullable ClassNode metadata(String className) {
        return read(className, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    }

    public static @Nullable ClassNode read(String className, int flags) {
        try {
            // ModLauncher 的字节码提供器要求使用正常的转换读取路径。
            return MixinService.getService().getBytecodeProvider().getClassNode(className, true, flags);
        } catch (ClassNotFoundException exception) {
            return null;
        } catch (IOException exception) {
            throw new IllegalStateException("读取 Mixin 字节码失败 " + className, exception);
        }
    }
}
