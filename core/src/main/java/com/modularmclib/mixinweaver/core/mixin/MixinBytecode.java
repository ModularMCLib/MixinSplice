package com.modularmclib.mixinweaver.core.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;

public final class MixinBytecode {

    private MixinBytecode() {}

    public static @Nullable ClassNode metadata(String className) {
        try {
            return MixinService.getService().getBytecodeProvider().getClassNode(className, false,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        } catch (ClassNotFoundException exception) {
            return null;
        } catch (IOException exception) {
            throw new IllegalStateException("读取 Mixin 字节码失败 " + className, exception);
        }
    }
}
