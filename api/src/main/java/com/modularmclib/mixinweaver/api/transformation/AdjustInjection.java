package com.modularmclib.mixinweaver.api.transformation;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 在声明者与对应 Mixin 共同作用的类上调整指定注入器。省略的属性沿用原值。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Repeatable(AdjustInjection.List.class)
public @interface AdjustInjection {

    /** 声明原注入器的 Mixin 二进制类名。动态目标副本使用原始类名。 */
    String mixin();

    /** 注入器处理方法的原始名称。重载时附加 JVM 方法描述符。 */
    String handler();

    /** 替换方法选择器。只指定 method 时清空接收方原有的 target。 */
    String[] method() default {};

    /** 替换描述符选择器。只指定 target 时清空接收方原有的 method。 */
    Desc[] target() default {};

    /** 替换注入点，由接收方的 Refmap 解析其中的引用。 */
    At[] at() default {};

    /** 替换选择范围。 */
    Slice[] slice() default {};

    /** 在完成 at 替换后，修改指定注入点的序号。 */
    AtOrdinal[] ordinals() default {};

    /** 最少需要成功的注入数，显式填写 -1 时恢复 Mixin 配置的默认值。 */
    int require() default -1;

    /** 开启注入次数调试时使用的期望值。 */
    int expect() default 1;

    /** 注入数上限，显式填写 -1 时恢复注入器的默认值。 */
    int allow() default -1;

    /** 移除处理方法上的注入器注解。 */
    boolean remove() default false;

    @Target({})
    @Retention(RetentionPolicy.CLASS)
    @interface AtOrdinal {

        /** 在当前注入点列表中的位置，从 0 开始。 */
        int index() default 0;

        /** -1 匹配全部位置，非负值匹配指定序号。 */
        int value();
    }

    /** 同一 Mixin 的多条注入调整声明。 */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @interface List {

        AdjustInjection[] value();
    }
}
