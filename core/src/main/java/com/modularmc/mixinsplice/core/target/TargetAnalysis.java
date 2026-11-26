package com.modularmc.mixinsplice.core.target;

import com.modularmc.mixinsplice.api.mixin.MixinSubject;
import com.modularmc.mixinsplice.api.target.DynamicTargets.Strategy;
import com.modularmc.mixinsplice.api.target.TargetContext;
import com.modularmc.mixinsplice.core.mixin.InjectionAnnotations;
import com.modularmc.mixinsplice.core.mixin.MixinBytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class TargetAnalysis implements TargetContext {

    private static final int INELIGIBLE = Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_ABSTRACT;

    private final MixinSubject mixin;
    private final ClassHierarchy hierarchy;
    private final IReferenceMapper refmap;
    private final Map<String, Optional<ClassNode>> classes = new HashMap<>();

    public TargetAnalysis(MixinSubject mixin, ClassHierarchy hierarchy, IReferenceMapper refmap) {
        this.mixin = mixin;
        this.hierarchy = hierarchy;
        this.refmap = refmap;
    }

    @Override
    public MixinSubject mixin() {
        return mixin;
    }

    @Override
    public List<String> subclassesOfDeclaredTargets() {
        return hierarchy.descendants(mixin.targets(), false);
    }

    @Override
    public List<String> implementersOfDeclaredTargets() {
        return hierarchy.descendants(mixin.targets(), true);
    }

    public Collection<String> select(Strategy strategy, ClassNode source) {
        return switch (strategy) {
            case SUBCLASSES -> subclassesOfDeclaredTargets();
            case IMPLEMENTERS -> implementersOfDeclaredTargets();
            case OVERRIDING, BYPASSING -> {
                List<String> selectors = InjectionAnnotations.selectors(source).toList();
                if (selectors.stream().anyMatch(selector -> selector.contains("<init>"))) {
                    // 构造器从不参与方法覆盖。每个子类都需要独立检查注入点。
                    yield Stream.concat(subclassesOfDeclaredTargets().stream(), implementersOfDeclaredTargets().stream()).distinct().toList();
                }
                Predicate<MethodNode> matches = selectors.isEmpty() || selectors.stream().anyMatch(TargetAnalysis::uncertain) ? method -> true : selectors.stream().map(this::methodFilter).reduce(method -> false, Predicate::or);
                yield findOverrides(matches, strategy == Strategy.BYPASSING);
            }
        };
    }

    @Override
    public List<String> subclassesOverriding(String method) {
        return findOverrides(methodFilter(method), false);
    }

    @Override
    public List<String> subclassesBypassing(String method) {
        return findOverrides(methodFilter(method), true);
    }

    @Override
    public boolean delegatesToSuper(String className, String method) {
        Predicate<MethodNode> filter = methodFilter(method);
        List<MethodNode> matching = methods(className).filter(filter).toList();
        return !matching.isEmpty() && matching.stream().allMatch(candidate -> delegates(className, candidate));
    }

    private List<String> findOverrides(Predicate<MethodNode> filter, boolean onlyBypassing) {
        return Stream.concat(subclassesOfDeclaredTargets().stream(), implementersOfDeclaredTargets().stream())
                .distinct()
                .filter(name -> methods(name).anyMatch(method -> filter.test(method) && overrides(name, method, onlyBypassing)))
                .toList();
    }

    private boolean overrides(String owner, MethodNode method, boolean onlyBypassing) {
        return mixin.targets().stream().filter(target -> hierarchy.isAncestor(owner, target))
                // 接口本身不应用注入器，Interface.super 调用也需要覆盖。
                .filter(target -> !onlyBypassing || hierarchy.isInterface(target) || !delegates(owner, method))
                .anyMatch(target -> Stream.concat(Stream.of(target), hierarchy.ancestors(target).stream()).anyMatch(
                        ancestor -> bytecode(ancestor).stream().flatMap(node -> node.methods.stream()).anyMatch(
                                inherited -> inherited.name.equals(method.name) && inherited.desc.equals(method.desc) && isAccessible(inherited, owner, ancestor))));
    }

    private static boolean isAccessible(MethodNode method, String child, String parent) {
        if ((method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) != 0) {
            return false;
        }
        return (method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0 || packageName(child).equals(packageName(parent));
    }

    private static String packageName(String className) {
        String name = className.replace('/', '.');
        return name.substring(0, name.lastIndexOf('.') + 1);
    }

    private boolean delegates(String owner, MethodNode method) {
        return SuperDelegation.onEveryPath(method, parent -> hierarchy.isAncestor(owner, parent));
    }

    private Stream<MethodNode> methods(String name) {
        return bytecode(name).stream().flatMap(node -> node.methods.stream())
                .filter(method -> (method.access & INELIGIBLE) == 0 && !method.name.startsWith("<"));
    }

    private Optional<ClassNode> bytecode(String name) {
        return classes.computeIfAbsent(name.replace('/', '.'), key -> Optional.ofNullable(MixinBytecode.read(key, 0)));
    }

    private Predicate<MethodNode> methodFilter(String selector) {
        String remapped = refmap.remap(mixin.className().replace('.', '/'), selector);
        MemberInfo member = MemberInfo.parse(remapped, null);
        return method -> method.name.equals(member.getName()) && (member.getDesc() == null || method.desc.equals(member.getDesc()));
    }

    private static boolean uncertain(String selector) {
        return selector.isBlank() || selector.startsWith("@") || selector.startsWith("/") || selector.contains("*") || selector.contains("{") || selector.contains("->");
    }
}
