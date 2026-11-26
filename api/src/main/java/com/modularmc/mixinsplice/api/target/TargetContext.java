package com.modularmc.mixinsplice.api.target;

import com.modularmc.mixinsplice.api.mixin.MixinSubject;

import java.util.List;

/** 提供动态目标选择所需的类层次和方法分析，不加载被检查的类。 */
public interface TargetContext {

    /** 当前 Mixin 的标识和已声明的目标。 */
    MixinSubject mixin();

    /** 返回类目标的直接和间接子类。 */
    List<String> subclassesOfDeclaredTargets();

    /** 返回接口目标的实现类，包括通过父类间接实现的类。 */
    List<String> implementersOfDeclaredTargets();

    /** 返回覆盖指定方法的子类和实现类。方法名可附带 JVM 描述符。 */
    List<String> subclassesOverriding(String method);

    /** 返回存在绕过父类路径的覆盖者。方法名可附带 JVM 描述符。 */
    List<String> subclassesBypassing(String method);

    /** 判断指定类中该方法的所有正常返回路径是否经过父实现。 */
    boolean delegatesToSuper(String className, String method);
}
