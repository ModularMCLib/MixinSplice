package com.modularmclib.mixinweaver.loader.neoforge.platform;

import com.modularmclib.mixinweaver.api.condition.ConditionResult;
import com.modularmclib.mixinweaver.core.platform.MavenModVersion;
import com.modularmclib.mixinweaver.core.platform.PlatformEnvironment;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.IModInfo;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

public final class NeoForgeEnvironment implements PlatformEnvironment {

    @Override
    public List<Path> classpathRoots() {
        LoadingModList loading = LoadingModList.get();
        if (loading == null) {
            return List.of();
        }
        var roots = new LinkedHashSet<Path>();
        loading.getModFiles().forEach(info -> roots.add(info.getFile().getFilePath()));
        roots.addAll(developmentRoots());
        return List.copyOf(roots);
    }

    // 开发运行的一个 Mod 可以由多个类目录和资源目录组成。
    private static List<Path> developmentRoots() {
        String foldersFile = System.getProperty("fml.modFoldersFile", "");
        if (!foldersFile.isEmpty()) {
            var folders = new Properties();
            try (var reader = Files.newBufferedReader(Path.of(foldersFile))) {
                folders.load(reader);
            } catch (IOException exception) {
                throw new UncheckedIOException("无法读取 NeoForge 开发运行的 Mod 路径 " + foldersFile, exception);
            }
            return folders.stringPropertyNames().stream().sorted()
                    .flatMap(mod -> Arrays.stream(folders.getProperty(mod).split(Pattern.quote(File.pathSeparator))))
                    .filter(path -> !path.isEmpty()).map(Path::of).toList();
        }
        String folders = System.getenv("MOD_CLASSES");
        if (folders == null) {
            folders = System.getProperty("fml.modFolders", "");
        }
        return Arrays.stream(folders.split(Pattern.quote(File.pathSeparator)))
                .filter(entry -> !entry.isEmpty()).map(entry -> {
                    int separator = entry.indexOf("%%");
                    return Path.of(separator < 0 ? entry : entry.substring(separator + 2));
                }).toList();
    }

    @Override
    public ConditionResult mod(String modId, String versionPredicate) {
        List<? extends IModInfo> mods = discoveredMods();
        return mods == null ? ConditionResult.UNKNOWN : ConditionResult.of(mods.stream().anyMatch(mod -> mod.getModId().equals(modId) && MavenModVersion.matches(mod.getVersion(), versionPredicate)));
    }

    @Override
    public Optional<String> modVersion(String modId) {
        List<? extends IModInfo> mods = discoveredMods();
        return mods == null ? Optional.empty() : mods.stream().filter(mod -> mod.getModId().equals(modId))
                .map(mod -> mod.getVersion().toString()).findFirst();
    }

    @Override
    public boolean matchesVersion(String version, String predicate) {
        return MavenModVersion.matches(version, predicate);
    }

    private static @Nullable List<? extends IModInfo> discoveredMods() {
        LoadingModList loading = LoadingModList.get();
        if (loading != null) {
            return loading.getMods();
        }
        ModList loaded = ModList.get();
        return loaded == null ? null : loaded.getMods();
    }
}
