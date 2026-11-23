package com.modularmclib.mixinsplice.api.target;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 在 Mixin 准备阶段根据已声明的目标生成额外的目标。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface DynamicTargets {

    Strategy value() default Strategy.BYPASSING;

    /** 选填的包名前缀。指定后仅在这些包及子包中生成目标。 */
    String[] packages() default {};

    /** 选填的自定义目标提供者 ID。存在时使用该提供者的结果。 */
    String provider() default "";

    /** 仅在生成的 Mixin 副本上允许注入点缺失。 */
    boolean relaxRequirements() default false;

    enum Strategy {
        BYPASSING,
        OVERRIDING,
        SUBCLASSES,
        IMPLEMENTERS
    }
}
