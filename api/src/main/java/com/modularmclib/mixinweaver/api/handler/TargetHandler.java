package com.modularmclib.mixinweaver.api.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 与注入器的 @mixinweaver:handler 选择器一起使用。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface TargetHandler {

    /** 声明目标处理方法的 Mixin 二进制类名。 */
    String mixin();

    /** 处理方法的原始名称，可附加 JVM 方法描述符以区分重载。 */
    String name();

    /** 选填的 Mixin 配置名。 */
    String config() default "";

    /** 选填的注入器前缀，例如 handler 或 redirect。 */
    String prefix() default "";
}
