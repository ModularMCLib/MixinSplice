package com.modularmclib.mixinweaver.loader.neoforge.platform;

import com.modularmclib.mixinweaver.api.condition.ConditionResult;
import com.modularmclib.mixinweaver.core.platform.PlatformEnvironment;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.IModInfo;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.List;

public final class NeoForgeEnvironment implements PlatformEnvironment {

    @Override
    public ConditionResult mod(String modId, String versionPredicate) {
        LoadingModList loading = LoadingModList.get();
        if (loading != null) {
            return check(loading.getMods(), modId, versionPredicate);
        }
        ModList loaded = ModList.get();
        return loaded == null ? ConditionResult.UNKNOWN : check(loaded.getMods(), modId, versionPredicate);
    }

    private static ConditionResult check(List<? extends IModInfo> mods, String modId, String versionPredicate) {
        return ConditionResult.of(mods.stream().anyMatch(mod -> mod.getModId().equals(modId) && (versionPredicate.isEmpty() || matches(mod.getVersion(), versionPredicate))));
    }

    private static boolean matches(ArtifactVersion actual, String predicate) {
        if (predicate.startsWith("[") || predicate.startsWith("(")) {
            try {
                return VersionRange.createFromVersionSpec(predicate).containsVersion(actual);
            } catch (InvalidVersionSpecificationException exception) {
                throw new IllegalArgumentException("无效的 NeoForge 版本范围 " + predicate, exception);
            }
        }
        for (String comparison : predicate.strip().split("\\s+")) {
            if (!matchesComparison(actual, comparison)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesComparison(ArtifactVersion actual, String comparison) {
        for (String operator : List.of(">=", "<=", ">", "<", "=")) {
            if (!comparison.startsWith(operator)) {
                continue;
            }
            int order = actual.compareTo(new DefaultArtifactVersion(comparison.substring(operator.length())));
            return switch (operator) {
                case ">=" -> order >= 0;
                case "<=" -> order <= 0;
                case ">" -> order > 0;
                case "<" -> order < 0;
                default -> order == 0;
            };
        }
        return actual.compareTo(new DefaultArtifactVersion(comparison)) == 0;
    }
}
