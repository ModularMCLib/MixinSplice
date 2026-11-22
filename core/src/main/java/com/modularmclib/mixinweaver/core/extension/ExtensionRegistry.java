package com.modularmclib.mixinweaver.core.extension;

import com.modularmclib.mixinweaver.api.condition.Condition;
import com.modularmclib.mixinweaver.api.extension.WeaverExtension;
import com.modularmclib.mixinweaver.api.mixin.MixinCanceller;
import com.modularmclib.mixinweaver.api.transformation.AnnotationTransformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

public final class ExtensionRegistry {

    private final List<WeaverExtension> ordered;
    private final Map<String, Condition> conditions;
    private final List<MixinCanceller> cancellers;
    private final List<AnnotationTransformation> transformations;

    public ExtensionRegistry(Collection<WeaverExtension> extensions) {
        ordered = order(extensions);
        Map<String, Condition> indexed = new HashMap<>();
        for (WeaverExtension extension : ordered) {
            extension.conditions().forEach((key, condition) -> {
                if (indexed.putIfAbsent(key, condition) != null) {
                    throw new IllegalArgumentException("重复的 MixinWeaver 条件 ID " + key);
                }
            });
        }
        conditions = Map.copyOf(indexed);
        cancellers = ordered.stream().flatMap(extension -> extension.cancellers().stream()).toList();
        transformations = ordered.stream().flatMap(extension -> extension.transformations().stream()).toList();
    }

    public static ExtensionRegistry discover() {
        return new ExtensionRegistry(ServiceLoader.load(WeaverExtension.class, WeaverExtension.class.getClassLoader())
                .stream().map(ServiceLoader.Provider::get).toList());
    }

    public List<WeaverExtension> ordered() {
        return ordered;
    }

    public Map<String, Condition> conditions() {
        return conditions;
    }

    public List<MixinCanceller> cancellers() {
        return cancellers;
    }

    public List<AnnotationTransformation> transformations() {
        return transformations;
    }

    private static List<WeaverExtension> order(Collection<WeaverExtension> extensions) {
        Map<String, WeaverExtension> byId = new TreeMap<>();
        Map<String, Set<String>> next = new HashMap<>();
        Map<String, Integer> incoming = new HashMap<>();
        for (WeaverExtension extension : extensions) {
            String id = extension.id();
            if (id.isBlank() || byId.putIfAbsent(id, extension) != null) {
                throw new IllegalArgumentException("无效或重复的 MixinWeaver 扩展 ID " + id);
            }
            next.put(id, new LinkedHashSet<>());
            incoming.put(id, 0);
        }
        byId.forEach((id, extension) -> {
            extension.before().forEach(other -> connect(id, other, next, incoming));
            extension.after().forEach(other -> connect(other, id, next, incoming));
        });

        PriorityQueue<String> ready = new PriorityQueue<>();
        incoming.forEach((id, count) -> {
            if (count == 0) {
                ready.add(id);
            }
        });
        List<WeaverExtension> sorted = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.remove();
            sorted.add(byId.get(id));
            for (String successor : next.get(id)) {
                if (incoming.compute(successor, (key, count) -> count - 1) == 0) {
                    ready.add(successor);
                }
            }
        }
        if (sorted.size() != byId.size()) {
            List<String> blocked = byId.keySet().stream().filter(id -> incoming.get(id) > 0).toList();
            throw new IllegalArgumentException("无法排序 MixinWeaver 扩展 " + blocked);
        }
        return List.copyOf(sorted);
    }

    private static void connect(String from, String to, Map<String, Set<String>> next, Map<String, Integer> incoming) {
        if (incoming.containsKey(from) && incoming.containsKey(to) && next.get(from).add(to)) {
            incoming.compute(to, (id, count) -> count + 1);
        }
    }
}
