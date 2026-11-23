package com.modularmclib.mixinsplice.api.mixin;

import java.util.List;

/** 应用或转换规则使用的 Mixin 标识。类名使用点分隔。 */
public record MixinSubject(String className, String configName, List<String> targets) {

    public MixinSubject {
        targets = List.copyOf(targets);
    }
}
