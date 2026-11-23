package com.modularmclib.mixinsplice.core.mixin;

import com.modularmclib.mixinsplice.api.mixin.MixinSubject;
import com.modularmclib.mixinsplice.api.target.DynamicTargetProvider;
import com.modularmclib.mixinsplice.api.target.DynamicTargets;
import com.modularmclib.mixinsplice.api.target.DynamicTargets.Strategy;
import com.modularmclib.mixinsplice.core.condition.ConditionEvaluator;
import com.modularmclib.mixinsplice.core.platform.PlatformEnvironment;
import com.modularmclib.mixinsplice.core.target.ClassHierarchy;
import com.modularmclib.mixinsplice.core.target.GeneratedMixin;
import com.modularmclib.mixinsplice.core.target.MixinCloner;
import com.modularmclib.mixinsplice.core.target.TargetAnalysis;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
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

    private final ILogger logger = MixinService.getService().getLogger("mixinsplice");
    private final ConditionEvaluator conditions;
    private final PlatformEnvironment platform;
    private final Map<String, DynamicTargetProvider> providers;
    private final Set<IMixinInfo> prepared = Collections.newSetFromMap(new IdentityHashMap<>());
    private @Nullable ClassHierarchy hierarchy;

    MixinPreparation(ConditionEvaluator conditions, PlatformEnvironment platform, Map<String, DynamicTargetProvider> providers) {
        this.conditions = conditions;
        this.platform = platform;
        this.providers = providers;
    }

    void prepare(Set<String> ownTargets, Set<String> otherTargets) {
        hierarchy = null;
        var targets = new LinkedHashSet<>(ownTargets);
        targets.addAll(otherTargets);
        var generated = new LinkedHashMap<IMixinConfig, List<GeneratedMixin>>();
        for (IMixinInfo mixin : MixinInternals.preparedMixins(targets)) {
            List<GeneratedMixin> copies = prepare(mixin);
            if (!copies.isEmpty()) {
                generated.computeIfAbsent(mixin.getConfig(), ignored -> new ArrayList<>()).addAll(copies);
            }
        }
        generated.forEach(MixinInternals::prepareGenerated);
        MixinInternals.listenForPluginMixins(mixin -> MixinInternals.prepareGenerated(mixin.getConfig(), prepare(mixin)));
    }

    private List<GeneratedMixin> prepare(IMixinInfo mixin) {
        if (!MixinBytecode.originalName(mixin).equals(mixin.getClassName()) || !prepared.add(mixin)) {
            return List.of();
        }
        ClassNode annotations = mixin.getClassNode(ClassReader.SKIP_CODE);
        AnnotationNode annotation = Annotations.getInvisible(annotations, DynamicTargets.class);
        if (annotation == null) {
            return List.of();
        }
        if (hierarchy == null) {
            hierarchy = new ClassHierarchy(platform.classpathRoots());
        }
        return expand(mixin, annotation, hierarchy);
    }

    private List<GeneratedMixin> expand(IMixinInfo mixin, AnnotationNode annotation, ClassHierarchy hierarchy) {
        IMixinConfig config = mixin.getConfig();
        var declared = mixin.getTargetClasses().stream().map(name -> name.replace('/', '.')).toList();
        var subject = new MixinSubject(mixin.getClassName(), config.getName(), declared);
        var context = new TargetAnalysis(subject, hierarchy, MixinInternals.referenceMapper(config));
        ClassNode bytecode = mixin.getClassNode(0);
        if (MixinCloner.isAccessorOnly(bytecode)) {
            logger.debug("跳过 {} 的动态副本，访问器由子类继承", subject.className());
            return List.of();
        }
        Strategy strategy = Annotations.getValue(annotation, "value", Strategy.class, Strategy.BYPASSING);
        String providerId = Annotations.getValue(annotation, "provider", "");
        boolean relax = Boolean.TRUE.equals(Annotations.getValue(annotation, "relaxRequirements"));
        List<String> packages = Annotations.getValue(annotation, "packages", true);
        String mixinPackage = config.getMixinPackage();

        Set<String> candidates = new LinkedHashSet<>(providerId.isEmpty() ? context.select(strategy, bytecode) : provider(providerId).select(context));
        candidates.removeAll(declared);
        var result = new ArrayList<GeneratedMixin>();
        IClassTracker tracker = MixinService.getService().getClassTracker();
        for (String target : candidates) {
            if (target.startsWith(mixinPackage) || hierarchy.isInterface(target) || (!packages.isEmpty() && packages.stream().noneMatch(prefix -> target.startsWith(prefix + ".")))) {
                logger.debug("跳过 {} -> {}，目标类型或包名不符合范围", subject.className(), target);
                continue;
            }
            if (tracker != null && tracker.isClassLoaded(target)) {
                logger.warn("跳过 {} -> {}，目标类已加载", subject.className(), target);
                continue;
            }
            if (!conditions.shouldApply(target, subject.className())) {
                logger.debug("跳过 {} -> {}，选择条件未满足", subject.className(), target);
                continue;
            }
            var plugin = config.getPlugin();
            if (plugin != null && !plugin.shouldApplyMixin(target, subject.className())) {
                logger.debug("跳过 {} -> {}，配置插件拒绝目标", subject.className(), target);
                continue;
            }
            GeneratedMixin copy = MixinCloner.clone(bytecode, mixinPackage, target, relax);
            if (copy != null) {
                result.add(copy);
                logger.debug("生成 {} -> {} 的动态副本 {}", subject.className(), target, copy.name());
            }
        }
        if ((bytecode.access & Opcodes.ACC_INTERFACE) != 0 && InjectionAnnotations.hasInjectionOrOverwrite(bytecode)) {
            declared.stream().filter(hierarchy::isInterface).forEach(target -> MixinInternals.removeTarget(mixin, target));
        }
        if (Boolean.getBoolean("mixinsplice.targets.debug")) {
            logger.info("{} 的 {} 个候选目标生成了 {} 个动态副本", subject.className(), candidates.size(), result.size());
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
}
