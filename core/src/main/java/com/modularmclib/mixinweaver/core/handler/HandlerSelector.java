package com.modularmclib.mixinweaver.core.handler;

import com.modularmclib.mixinweaver.api.handler.TargetHandler;
import com.modularmclib.mixinweaver.core.mixin.MixinBytecode;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;
import org.spongepowered.asm.mixin.injection.selectors.InvalidSelectorException;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotatedElement;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.asm.util.asm.MethodNodeEx;

@ITargetSelectorDynamic.SelectorId(namespace = "mixinweaver", value = "handler")
public final class HandlerSelector implements ITargetSelectorDynamic {

    private final String mixin;
    private final String config;
    private final String prefix;
    private final MemberInfo member;

    private HandlerSelector(String mixin, String config, String prefix, MemberInfo member) {
        this.mixin = mixin.replace('/', '.');
        this.config = config;
        this.prefix = prefix;
        this.member = member;
    }

    /** Mixin 的动态选择器解析入口。 */
    public static HandlerSelector parse(String input, ISelectorContext context) {
        IAnnotationHandle annotation = switch (context.getMethod()) {
            case MethodNode node -> Annotations.handleOf(Annotations.getInvisible(node, TargetHandler.class));
            case IAnnotatedElement element -> element.getAnnotation(TargetHandler.class);
            default -> throw new InvalidSelectorException("无法读取 @TargetHandler");
        };
        if (annotation == null || !annotation.exists()) {
            throw new InvalidSelectorException("@mixinweaver:handler 需要 @TargetHandler");
        }
        String mixin = annotation.getValue("mixin", "");
        String name = annotation.getValue("name", "");
        if (mixin.isBlank() || name.isBlank()) {
            throw new InvalidSelectorException("@TargetHandler 需要声明 Mixin 和方法");
        }
        return new HandlerSelector(mixin, annotation.getValue("config", ""), annotation.getValue("prefix", ""),
                MemberInfo.parse(name, context));
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
        if (!(node.getMethod() instanceof MethodNodeEx method) || !mixin.equals(MixinBytecode.originalName(method.getOwner())) || Annotations.getVisible(method, MixinMerged.class) == null || (!config.isEmpty() && !config.equals(method.getOwner().getConfig().getName())) || (!prefix.isEmpty() && !method.name.startsWith(prefix + "$"))) {
            return MatchResult.NONE;
        }
        if (member.getName().equals(method.getOriginalName()) && (member.getDesc() == null || member.getDesc().equals(method.desc))) {
            return MatchResult.EXACT_MATCH;
        }
        return MatchResult.NONE;
    }

    @Override
    public String toString() {
        return "@mixinweaver:handler(" + mixin + "#" + member + ")";
    }
}
