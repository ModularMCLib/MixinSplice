package com.modularmclib.mixinweaver.core.target;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClassHierarchy {

    private final Map<String, Entry> classes = new LinkedHashMap<>();
    private final Map<String, List<Entry>> children = new LinkedHashMap<>();

    public ClassHierarchy(List<Path> roots) {
        for (Path root : roots) {
            try {
                if (Files.isDirectory(root)) {
                    index(root);
                } else {
                    try (var fileSystem = FileSystems.newFileSystem(root)) {
                        index(fileSystem.getPath("/"));
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("无法扫描动态 Mixin 的类路径 " + root, exception);
            }
        }
        for (Entry child : classes.values()) {
            if (child.superName != null) {
                children.computeIfAbsent(child.superName, name -> new ArrayList<>()).add(child);
            }
            for (String parent : child.interfaces) {
                children.computeIfAbsent(parent, name -> new ArrayList<>()).add(child);
            }
        }
    }

    public List<String> descendants(Collection<String> parents, boolean interfaces) {
        var visited = new HashSet<String>();
        var pending = new ArrayDeque<Entry>();
        for (String parent : parents) {
            String name = internal(parent);
            Entry entry = classes.get(name);
            if (entry != null && entry.isInterface() == interfaces) {
                pending.addAll(children.getOrDefault(name, List.of()));
            }
        }

        var found = new ArrayList<String>();
        while (!pending.isEmpty()) {
            Entry entry = pending.remove();
            if (!visited.add(entry.name)) {
                continue;
            }
            pending.addAll(children.getOrDefault(entry.name, List.of()));
            if (!entry.isInterface()) {
                found.add(binary(entry.name));
            }
        }
        return found.stream().sorted().toList();
    }

    public boolean isInterface(String className) {
        Entry entry = classes.get(internal(className));
        return entry != null && entry.isInterface();
    }

    public boolean isAncestor(String className, String ancestor) {
        return ancestors(className).contains(internal(ancestor));
    }

    public Set<String> ancestors(String className) {
        Set<String> visited = new HashSet<>();
        var pending = new ArrayDeque<String>();
        addParents(classes.get(internal(className)), pending);
        while (!pending.isEmpty()) {
            String name = pending.remove();
            if (visited.add(name)) {
                addParents(classes.get(name), pending);
            }
        }
        return visited;
    }

    private static void addParents(@Nullable Entry entry, ArrayDeque<String> pending) {
        if (entry != null) {
            if (entry.superName != null) {
                pending.add(entry.superName);
            }
            pending.addAll(entry.interfaces);
        }
    }

    private void index(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            for (var iterator = files.filter(path -> path.getFileName() != null && path.getFileName().toString().endsWith(".class") && !path.toString().contains("META-INF/versions/") && !path.toString().contains("META-INF\\versions\\")).iterator(); iterator.hasNext();) {
                Path path = iterator.next();
                try (var input = Files.newInputStream(path)) {
                    ClassReader reader = new ClassReader(input);
                    String name = reader.getClassName();
                    classes.putIfAbsent(name, new Entry(name, reader.getSuperName(), List.of(reader.getInterfaces()), reader.getAccess()));
                }
            }
        }
    }

    private static String internal(String name) {
        return name.replace('.', '/');
    }

    private static String binary(String name) {
        return name.replace('/', '.');
    }

    private record Entry(String name, @Nullable String superName, List<String> interfaces, int access) {

        private boolean isInterface() {
            return (access & Opcodes.ACC_INTERFACE) != 0;
        }
    }
}
