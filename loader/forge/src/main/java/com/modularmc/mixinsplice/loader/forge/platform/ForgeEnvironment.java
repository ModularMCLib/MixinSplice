package com.modularmc.mixinsplice.loader.forge.platform;

import com.modularmc.mixinsplice.api.condition.ConditionResult;
import com.modularmc.mixinsplice.core.platform.MavenModVersion;
import com.modularmc.mixinsplice.core.platform.PlatformEnvironment;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class ForgeEnvironment implements PlatformEnvironment {

    @Override
    public List<Path> classpathRoots() {
        LoadingModList loading = LoadingModList.get();
        if (loading == null) {
            return List.of();
        }
        return loading.getModFiles().stream().map(info -> info.getFile().getSecureJar().getRootPath()).distinct().toList();
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
