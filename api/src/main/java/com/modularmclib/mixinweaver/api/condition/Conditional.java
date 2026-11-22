package com.modularmclib.mixinweaver.api.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 在 Mixin 配置插件选择目标时评估的限制。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Conditional {

    /** 每个条件都必须满足。 */
    ConditionSpec[] require() default {};

    /** 任何一个条件满足时取消应用。 */
    ConditionSpec[] conflict() default {};

    /** 已知条件无法决定应用资格时采用的策略。 */
    UnknownPolicy unknown() default UnknownPolicy.DENY;
}
