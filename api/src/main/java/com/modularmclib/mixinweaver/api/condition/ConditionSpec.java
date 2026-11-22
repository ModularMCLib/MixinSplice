package com.modularmclib.mixinweaver.api.condition;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 声明供选择阶段读取的条件。 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface ConditionSpec {

    /** Mod ID、Mixin 的二进制类名或已注册的自定义条件 ID。 */
    String value() default "";

    /** 默认按 Mod ID 求值。 */
    Kind kind() default Kind.MOD;

    /** 公共无参构造器的自定义条件类。指定后无需注册 ID。 */
    Class<? extends Condition> tester() default Condition.class;

    /** 平台版本谓词。空字符串表示只检查 Mod 是否存在。 */
    String version() default "";

    /** 任一版本谓词满足即可。仅用于 Mod 条件，与 version 二选一。 */
    String[] versions() default {};

    enum Kind {
        MOD,
        MIXIN,
        CUSTOM
    }
}
