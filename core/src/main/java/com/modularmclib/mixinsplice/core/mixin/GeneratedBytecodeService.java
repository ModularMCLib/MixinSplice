package com.modularmclib.mixinsplice.core.mixin;

import com.modularmclib.mixinsplice.core.target.GeneratedMixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 只在注册生成的 Mixin 时替换该配置的字节码提供器。 */
final class GeneratedBytecodeService {

    private GeneratedBytecodeService() {}

    static IMixinService wrap(IMixinService base, List<GeneratedMixin> generated) {
        var byName = generated.stream().collect(Collectors.toUnmodifiableMap(GeneratedMixin::name, Function.identity()));
        var provider = base.getBytecodeProvider();
        IClassBytecodeProvider decorated = IClassBytecodeProvider.class.cast(Proxy.newProxyInstance(
                IClassBytecodeProvider.class.getClassLoader(), new Class<?>[] { IClassBytecodeProvider.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getClassNode")) {
                        GeneratedMixin mixin = byName.get(((String) args[0]).replace('/', '.'));
                        if (mixin != null) {
                            ClassNode node = new ClassNode();
                            int flags = args.length == 3 ? (int) args[2] : 0;
                            new ClassReader(mixin.bytes()).accept(node, flags);
                            return node;
                        }
                    }
                    return invoke(provider, method, args);
                }));
        return withProvider(base, decorated);
    }

    static IMixinService filter(IMixinService base, BiFunction<String, ClassNode, ClassNode> filter) {
        IClassBytecodeProvider delegate = base.getBytecodeProvider();
        IClassBytecodeProvider provider = IClassBytecodeProvider.class.cast(Proxy.newProxyInstance(
                IClassBytecodeProvider.class.getClassLoader(), new Class<?>[] { IClassBytecodeProvider.class },
                (proxy, method, args) -> {
                    Object result = invoke(delegate, method, args);
                    return result instanceof ClassNode node ? filter.apply((String) args[0], node) : result;
                }));
        return withProvider(base, provider);
    }

    private static IMixinService withProvider(IMixinService base, IClassBytecodeProvider provider) {
        return IMixinService.class.cast(Proxy.newProxyInstance(
                IMixinService.class.getClassLoader(), new Class<?>[] { IMixinService.class },
                (proxy, method, args) -> method.getName().equals("getBytecodeProvider") ? provider : invoke(base, method, args)));
    }

    private static Object invoke(Object target, Method method, Object[] arguments) throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
