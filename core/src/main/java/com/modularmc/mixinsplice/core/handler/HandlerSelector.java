package com.modularmc.mixinsplice.core.handler;

import com.modularmc.mixinsplice.api.handler.TargetHandler;
import com.modularmc.mixinsplice.core.mixin.MixinBytecode;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.asm.util.asm.MethodNodeEx;

@ITargetSelectorDynamic.SelectorId(namespace = "mixinsplice", value = "handler")
public final class HandlerSelector implements ITargetSelectorDynamic {

    private final String mixin;
    private final String config;
    private final String prefix;
    private final MemberInfo member;
    private final boolean print;

    private HandlerSelector(String mixin, String config, String prefix, MemberInfo member, boolean print) {
        this.mixin = mixin.replace('/', '.');
        this.config = config;
        this.prefix = prefix;
        this.member = member;
        this.print = print;
    }

    /** Mixin 的动态选择器解析入口。 */
    public static HandlerSelector parse(String input, ISelectorContext context) {
        IAnnotationHandle annotation = switch (context.getMethod()) {
            case MethodNode node -> Annotations.handleOf(Annotations.getInvisible(node, TargetHandler.class));
            case IAnnotatedElement element -> element.getAnnotation(TargetHandler.class);
            default -> throw new InvalidSelectorException("无法读取 @TargetHandler");
        };
        if (annotation == null || !annotation.exists()) {
            throw new InvalidSelectorException("@mixinsplice:handler 需要 @TargetHandler");
        }
        String mixin = annotation.getValue("mixin", "");
        String name = annotation.getValue("name", "");
        if (mixin.isBlank() || name.isBlank()) {
            throw new InvalidSelectorException("@TargetHandler 需要声明 Mixin 和方法");
        }
        MemberInfo member = MemberInfo.parse(name, context);
        if (context.getMethod() instanceof IAnnotatedElement) {
            HandlerCompilation.addMapping(context, mixin, name, member);
        }
        return new HandlerSelector(mixin, annotation.getValue("config", ""), annotation.getValue("prefix", ""),
                member, annotation.getValue("print", false));
    }

    @Override
    public @Nullable ITargetSelector next() {
        return null;
    }

    @Override
    public ITargetSelector configure(Configure request, String... args) {
        return this;
    }

    @Override
    public ITargetSelector validate() {
        member.validate();
        return this;
    }

    @Override
    public ITargetSelector attach(ISelectorContext context) {
        if (mixin.equals(MixinBytecode.originalName(context.getMixin().getMixin()))) {
            throw new InvalidSelectorException("处理方法选择器必须引用另一个 Mixin");
        }
        if (print && context instanceof SpecialMethodInfo info) {
            var target = info.getTargetClassNode();
            var logger = MixinService.getService().getLogger("mixinsplice");
            logger.info("{} 在 {} 上的候选处理方法", this, target.name);
            for (MethodNode method : target.methods) {
                if (method instanceof MethodNodeEx merged && Annotations.getVisible(merged, MixinMerged.class) != null) {
                    logger.info("  {} {}{} -> {} {}", MixinBytecode.originalName(merged.getOwner()), merged.getOriginalName(),
                            merged.desc, merged.name, matches(merged) ? "匹配" : "不匹配");
                }
            }
        }
        return this;
    }

    @Override
    public int getMinMatchCount() {
        return 0;
    }

    @Override
    public int getMaxMatchCount() {
        return 1;
    }

    @Override
    public <T> MatchResult match(ElementNode<T> node) {
        return !print && node.getMethod() instanceof MethodNodeEx method && Annotations.getVisible(method, MixinMerged.class) != null && matches(method) ? MatchResult.EXACT_MATCH : MatchResult.NONE;
    }

    private boolean matches(MethodNodeEx method) {
        return mixin.equals(MixinBytecode.originalName(method.getOwner())) && (config.isEmpty() || config.equals(method.getOwner().getConfig().getName())) && (prefix.isEmpty() || method.name.startsWith(prefix + "$")) && member.getName().equals(method.getOriginalName()) && (member.getDesc() == null || member.getDesc().equals(method.desc));
    }

    @Override
    public String toString() {
        return "@mixinsplice:handler(" + mixin + "#" + member + ")";
    }
}
