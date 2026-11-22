package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.api.condition.Conditional;
import com.modularmclib.mixinweaver.api.mixin.MixinSubject;
import com.modularmclib.mixinweaver.api.target.DynamicTargetProvider;
import com.modularmclib.mixinweaver.api.target.DynamicTargets;
import com.modularmclib.mixinweaver.api.target.DynamicTargets.Strategy;
import com.modularmclib.mixinweaver.core.condition.ConditionEvaluator;
import com.modularmclib.mixinweaver.core.platform.PlatformEnvironment;
import com.modularmclib.mixinweaver.core.target.ClassHierarchy;
import com.modularmclib.mixinweaver.core.target.GeneratedMixin;
import com.modularmclib.mixinweaver.core.target.MixinCloner;
import com.modularmclib.mixinweaver.core.target.TargetAnalysis;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MixinPreparation {

    private final ConditionEvaluator conditions;
    private final PlatformEnvironment platform;
    private final Map<String, DynamicTargetProvider> providers;
    private final Set<IMixinInfo> prepared = Collections.newSetFromMap(new IdentityHashMap<>());

    MixinPreparation(ConditionEvaluator conditions, PlatformEnvironment platform, Map<String, DynamicTargetProvider> providers) {
        this.conditions = conditions;
        this.platform = platform;
        this.providers = providers;
    }

    void prepare(Set<String> ownTargets, Set<String> otherTargets) {
        var targets = new LinkedHashSet<>(ownTargets);
        targets.addAll(otherTargets);
        var generated = new LinkedHashMap<IMixinConfig, List<GeneratedMixin>>();
        var dynamic = new ArrayList<DynamicSource>();
        for (IMixinInfo mixin : MixinInternals.preparedMixins(targets)) {
            if (!prepared.add(mixin)) {
                continue;
            }
            ClassNode annotations = mixin.getClassNode(ClassReader.SKIP_CODE);
            if (Annotations.getInvisible(annotations, Conditional.class) != null) {
                for (String name : mixin.getTargetClasses()) {
                    String target = name.replace('/', '.');
                    if (!conditions.shouldApply(target, mixin.getClassName())) {
                        MixinInternals.removeTarget(mixin, target);
                    }
                }
            }
            AnnotationNode annotation = Annotations.getInvisible(annotations, DynamicTargets.class);
            if (annotation != null) {
                dynamic.add(new DynamicSource(mixin, annotation));
            }
        }
        if (dynamic.isEmpty()) {
            return;
        }

        ClassHierarchy hierarchy = new ClassHierarchy(platform.classpathRoots());
        for (DynamicSource source : dynamic) {
            IMixinConfig config = source.mixin.getConfig();
            generated.computeIfAbsent(config, ignored -> new ArrayList<>()).addAll(expand(source, hierarchy));
        }
        generated.forEach(MixinInternals::prepareGenerated);
    }

    private List<GeneratedMixin> expand(DynamicSource source, ClassHierarchy hierarchy) {
        IMixinInfo mixin = source.mixin;
        IMixinConfig config = mixin.getConfig();
        var declared = mixin.getTargetClasses().stream().map(name -> name.replace('/', '.')).toList();
        var subject = new MixinSubject(mixin.getClassName(), config.getName(), declared);
        var context = new TargetAnalysis(subject, hierarchy, MixinInternals.referenceMapper(config));
        ClassNode bytecode = mixin.getClassNode(0);
        Strategy strategy = Annotations.getValue(source.annotation, "value", Strategy.class, Strategy.BYPASSING);
        String providerId = Annotations.getValue(source.annotation, "provider", "");
        boolean relax = Boolean.TRUE.equals(Annotations.getValue(source.annotation, "relaxRequirements"));
        List<String> packages = Annotations.getValue(source.annotation, "packages", true);
        String mixinPackage = config.getMixinPackage();

        Set<String> candidates = new LinkedHashSet<>(providerId.isEmpty() ? context.select(strategy, bytecode) : provider(providerId).select(context));
        candidates.removeAll(declared);
        var result = new ArrayList<GeneratedMixin>();
        IClassTracker tracker = MixinService.getService().getClassTracker();
        for (String target : candidates) {
            if (target.startsWith(mixinPackage) || hierarchy.isInterface(target) || (!packages.isEmpty() && packages.stream().noneMatch(prefix -> target.startsWith(prefix + "."))) || (tracker != null && tracker.isClassLoaded(target)) || !conditions.shouldApply(target, mixin.getClassName())) {
                continue;
            }
            var plugin = config.getPlugin();
            if (plugin == null || plugin.shouldApplyMixin(target, mixin.getClassName())) {
                result.add(MixinCloner.clone(bytecode, mixinPackage, target, relax));
            }
        }
        if (!result.isEmpty() && bytecode.methods.stream().anyMatch(method -> method.visibleAnnotations != null && method.visibleAnnotations.stream().anyMatch(annotation -> annotation.desc.startsWith(
                "Lorg/spongepowered/asm/mixin/injection/")))) {
            declared.stream().filter(hierarchy::isInterface).forEach(target -> MixinInternals.removeTarget(mixin, target));
        }
        return result;
    }

    private DynamicTargetProvider provider(String id) {
        DynamicTargetProvider provider = providers.get(id);
        if (provider == null) {
            throw new IllegalArgumentException("未注册的动态目标提供者 " + id);
        }
        return provider;
    }

    private record DynamicSource(IMixinInfo mixin, AnnotationNode annotation) {}
}
