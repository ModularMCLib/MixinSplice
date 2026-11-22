package com.modularmclib.mixinweaver.api.transformation;

import com.modularmclib.mixinweaver.api.mixin.MixinSubject;

import java.util.Optional;

/** 指明注解所在的 Mixin 和可选的方法。 */
public record AnnotationTarget(MixinSubject mixin, Optional<Method> method) {

    public record Method(String name, String descriptor) {}
}
