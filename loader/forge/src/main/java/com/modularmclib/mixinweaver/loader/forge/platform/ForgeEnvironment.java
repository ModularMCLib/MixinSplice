package com.modularmclib.mixinweaver.loader.forge.platform;

import com.modularmclib.mixinweaver.api.condition.ConditionResult;
import com.modularmclib.mixinweaver.core.platform.MavenModVersion;
import com.modularmclib.mixinweaver.core.platform.PlatformEnvironment;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.List;

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
        LoadingModList loading = LoadingModList.get();
        if (loading != null) {
            return check(loading.getMods(), modId, versionPredicate);
        }
        ModList loaded = ModList.get();
        return loaded == null ? ConditionResult.UNKNOWN : check(loaded.getMods(), modId, versionPredicate);
    }

    private static ConditionResult check(List<? extends IModInfo> mods, String modId, String predicate) {
        return ConditionResult.of(mods.stream().anyMatch(mod -> mod.getModId().equals(modId) && MavenModVersion.matches(mod.getVersion(), predicate)));
    }
}
