package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.api.mixin.MixinCanceller;
import com.modularmclib.mixinweaver.api.mixin.MixinSubject;
import com.modularmclib.mixinweaver.api.transformation.AnnotationTransformation;
import com.modularmclib.mixinweaver.core.condition.ConditionEvaluator;
import com.modularmclib.mixinweaver.core.extension.ExtensionRegistry;
import com.modularmclib.mixinweaver.core.handler.HandlerSelector;
import com.modularmclib.mixinweaver.core.transformation.AnnotationTransformer;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.service.MixinService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MixinWeaverExtension implements IExtension {

    private static boolean installed;

    private final List<MixinCanceller> cancellers;
    private final List<AnnotationTransformation> transformations;
    private final AnnotationTransformer transformer;
    private final ConditionalTargets conditionalTargets;
    private final ILogger logger = MixinService.getService().getLogger("mixinweaver");
    private final Set<IMixinInfo> adjusted = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Runnable> scopedChanges = new ArrayList<>();

    private MixinWeaverExtension(ExtensionRegistry registry, ConditionEvaluator evaluator) {
        cancellers = registry.cancellers();
        transformations = registry.transformations();
        transformer = new AnnotationTransformer(transformations);
        conditionalTargets = new ConditionalTargets(evaluator);
    }

    static synchronized void install(ExtensionRegistry registry, ConditionEvaluator evaluator) {
        if (installed) {
            return;
        }
        Extensions extensions = extensions();
        MixinInternals.verifyApplicationAccess();
        TargetSelector.register(HandlerSelector.class, "mixinweaver");
        MixinInternals.registerBeforeConsumers(extensions, new MixinWeaverExtension(registry, evaluator));
        installed = true;
    }

    static Extensions extensions() {
        Object active = MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
        if (active instanceof IMixinTransformer transformer && transformer.getExtensions() instanceof Extensions registry) {
            return registry;
        }
        throw new IllegalStateException("MixinWeaver 需要 Mixin 0.8.7 的转换扩展注册表");
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        MixinInternals.filterTargets(conditionalTargets::filter);
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
        // Mixin 0.8.7 串行应用目标。上一个目标出错时可能跳过 postApply。
        restoreScopedChanges();
        try {
            prepareApplication(context);
        } catch (RuntimeException | Error failure) {
            restoreScopedChanges();
            throw failure;
        }
    }

    private void prepareApplication(ITargetClassContext context) {
        String target = context.getClassNode().name.replace('/', '.');
        var mixins = MixinInternals.activeMixins(context);
        var selected = new ArrayList<ApplicationMixin>();
        for (var iterator = mixins.iterator(); iterator.hasNext();) {
            IMixinInfo info = IMixinInfo.class.cast(iterator.next());
            if (!cancellers.isEmpty() && isCancelled(info, target)) {
                iterator.remove();
                logger.debug("已取消 {} 对 {} 的 Mixin 应用", info.getClassName(), target);
                continue;
            }
            selected.add(new ApplicationMixin(info, MixinInternals.originalBytecode(info), MixinBytecode.originalName(info)));
        }

        var cancelled = new LinkedHashSet<String>();
        selected.forEach(mixin -> cancelled.addAll(MixinDeclarations.cancellations(mixin.bytecode)));
        selected.removeIf(mixin -> {
            if (!cancelled.contains(mixin.originalName)) {
                return false;
            }
            mixins.remove(mixin.info);
            logger.debug("已取消 {} 对 {} 的 Mixin 应用", mixin.originalName, target);
            return true;
        });

        var adjustments = new LinkedHashMap<String, List<InjectionAdjustment>>();
        for (ApplicationMixin mixin : selected) {
            for (InjectionAdjustment rule : MixinDeclarations.adjustments(mixin.bytecode)) {
                adjustments.computeIfAbsent(rule.mixin(), ignored -> new ArrayList<>()).add(rule);
            }
        }
        for (ApplicationMixin mixin : selected) {
            if (!transformations.isEmpty() && !adjusted.contains(mixin.info)) {
                transformer.transform(subject(mixin.info), mixin.bytecode);
                adjusted.add(mixin.info);
            }
            for (InjectionAdjustment rule : adjustments.getOrDefault(mixin.originalName, List.of())) {
                rule.apply(mixin.info, mixin.bytecode, scopedChanges);
            }
            MixinDeclarations.hideMetadata(mixin.bytecode, scopedChanges);
        }
    }

    private boolean isCancelled(IMixinInfo info, String target) {
        MixinSubject mixin = subject(info);
        return cancellers.stream().anyMatch(rule -> rule.shouldCancel(mixin, target));
    }

    private static MixinSubject subject(IMixinInfo info) {
        return new MixinSubject(info.getClassName(), info.getConfig().getName(),
                info.getTargetClasses().stream().map(name -> name.replace('/', '.')).toList());
    }

    @Override
    public void postApply(ITargetClassContext context) {
        restoreScopedChanges();
    }

    private void restoreScopedChanges() {
        scopedChanges.reversed().forEach(Runnable::run);
        scopedChanges.clear();
    }

    @Override
    public void export(MixinEnvironment environment, String name, boolean force, ClassNode classNode) {}

    private record ApplicationMixin(IMixinInfo info, ClassNode bytecode, String originalName) {}
}
