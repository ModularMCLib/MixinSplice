package com.modularmclib.mixinsplice.api.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 在声明者通过选择的目标类上取消其他 Mixin。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface CancelMixins {

    /** 待取消的 Mixin 二进制类名，包括以原始类名标识的动态目标副本。 */
    String[] value();
}
