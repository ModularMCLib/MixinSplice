package com.modularmclib.mixinweaver.api.transformation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 在声明者与对应 Mixin 共同作用的类上替换注入目标选择器。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Repeatable(AdjustInjection.List.class)
public @interface AdjustInjection {

    /** 声明原注入器的 Mixin 二进制类名。动态目标副本使用原始类名。 */
    String mixin();

    /** 注入器处理方法的原始名称。重载时附加 JVM 方法描述符。 */
    String handler();

    /** 替换原 method 和 target 的方法选择器，使用接收方的 Refmap 解析。 */
    String[] method();

    /** 同一 Mixin 的多条注入调整声明。 */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @interface List {

        AdjustInjection[] value();
    }
}
