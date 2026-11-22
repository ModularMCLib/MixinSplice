package com.modularmclib.mixinweaver.api.condition;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 声明供选择阶段读取的条件。 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface ConditionSpec {

    Kind kind();

    /** Mod ID、Mixin 的二进制类名或已注册的自定义条件 ID。 */
    String value();

    /** 平台版本谓词。空字符串表示只检查 Mod 是否存在。 */
    String version() default "";

    enum Kind {
        MOD,
        MIXIN,
        CUSTOM
    }
}
