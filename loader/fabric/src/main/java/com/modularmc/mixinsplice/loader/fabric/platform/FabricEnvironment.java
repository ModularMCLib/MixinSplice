package com.modularmc.mixinsplice.loader.fabric.platform;

import com.modularmc.mixinsplice.api.condition.ConditionResult;
import com.modularmc.mixinsplice.core.platform.PlatformEnvironment;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class FabricEnvironment implements PlatformEnvironment {

    @Override
    public List<Path> classpathRoots() {
        return FabricLoader.getInstance().getAllMods().stream()
                .filter(mod -> !mod.getMetadata().getId().equals("java"))
                .flatMap(mod -> mod.getRootPaths().stream()).distinct().toList();
    }

    @Override
    public ConditionResult mod(String modId, String versionPredicate) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> ConditionResult.of(matches(mod.getMetadata().getVersion(), versionPredicate)))
                .orElse(ConditionResult.FALSE);
    }

    @Override
    public Optional<String> modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(mod -> mod.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public boolean matchesVersion(String version, String predicate) {
        try {
            return matches(Version.parse(version), predicate);
        } catch (VersionParsingException exception) {
            throw new IllegalArgumentException("无效的 Fabric Mod 版本 " + version, exception);
        }
    }

    private static boolean matches(Version actual, String predicate) {
        try {
            return predicate.isBlank() || VersionPredicate.parse(predicate).test(actual);
        } catch (VersionParsingException exception) {
            throw new IllegalArgumentException("无效的 Fabric Mod 版本谓词 " + predicate, exception);
        }
    }
}
