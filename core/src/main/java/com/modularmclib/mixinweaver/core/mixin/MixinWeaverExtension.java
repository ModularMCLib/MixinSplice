package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.api.mixin.MixinCanceller;
import com.modularmclib.mixinweaver.api.mixin.MixinSubject;
import com.modularmclib.mixinweaver.api.transformation.AnnotationTransformation;
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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

final class MixinWeaverExtension implements IExtension {

    private static boolean installed;

    private final List<MixinCanceller> cancellers;
    private final List<AnnotationTransformation> transformations;
    private final AnnotationTransformer transformer;
    private final ILogger logger = MixinService.getService().getLogger("mixinweaver");
    private final Set<IMixinInfo> adjusted = Collections.newSetFromMap(new IdentityHashMap<>());

    private MixinWeaverExtension(ExtensionRegistry registry) {
        cancellers = registry.cancellers();
        transformations = registry.transformations();
        transformer = new AnnotationTransformer(transformations);
    }

    static synchronized void install(ExtensionRegistry registry) {
        if (installed) {
            return;
        }
        Extensions extensions = extensions();
        var extension = new MixinWeaverExtension(registry);
        if (!extension.cancellers.isEmpty() || !extension.transformations.isEmpty()) {
            MixinInternals.verifyApplicationAccess();
        }
        TargetSelector.register(HandlerSelector.class, "mixinweaver");
        extensions.add(extension);
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
        return !cancellers.isEmpty() || !transformations.isEmpty();
    }

    @Override
    public void preApply(ITargetClassContext context) {
        String target = context.getClassNode().name.replace('/', '.');
        var mixins = MixinInternals.activeMixins(context);
        for (var iterator = mixins.iterator(); iterator.hasNext();) {
            IMixinInfo info = IMixinInfo.class.cast(iterator.next());
            MixinSubject mixin = new MixinSubject(info.getClassName(), info.getConfig().getName(),
                    info.getTargetClasses().stream().map(name -> name.replace('/', '.')).toList());
            if (cancellers.stream().anyMatch(rule -> rule.shouldCancel(mixin, target))) {
                iterator.remove();
                logger.debug("已取消 {} 对 {} 的 Mixin 应用", mixin.className(), target);
            } else if (!transformations.isEmpty() && !adjusted.contains(info)) {
                transformer.transform(mixin, MixinInternals.originalBytecode(info));
                adjusted.add(info);
            }
        }
    }

    @Override
    public void postApply(ITargetClassContext context) {}

    @Override
    public void export(MixinEnvironment environment, String name, boolean force, ClassNode classNode) {}
}
