package com.modularmclib.mixinweaver.core.platform;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.math.BigInteger;
import java.util.List;

/** Forge 和 NeoForge 使用 Maven 版本对象，在此统一解释比较式和区间。 */
public final class MavenModVersion {

    private MavenModVersion() {}

    public static boolean matches(ArtifactVersion actual, String expression) {
        String predicate = expression.strip();
        if (predicate.isEmpty() || predicate.equals("*")) {
            return true;
        }
        if (predicate.startsWith("[") || predicate.startsWith("(")) {
            try {
                return VersionRange.createFromVersionSpec(predicate).containsVersion(actual);
            } catch (InvalidVersionSpecificationException exception) {
                throw new IllegalArgumentException("无效的 Mod 版本范围 " + expression, exception);
            }
        }
        for (String comparison : predicate.split("\\s+")) {
            if (!matchesComparison(actual, comparison)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesComparison(ArtifactVersion actual, String comparison) {
        if (comparison.matches("\\d+(\\.\\d+)*\\.[xX*]")) {
            String prefix = comparison.substring(0, comparison.length() - 2);
            int separator = prefix.lastIndexOf('.') + 1;
            String upper = prefix.substring(0, separator) + new BigInteger(prefix.substring(separator)).add(BigInteger.ONE);
            return actual.compareTo(new DefaultArtifactVersion(prefix)) >= 0 && actual.compareTo(new DefaultArtifactVersion(upper)) < 0;
        }
        for (String operator : List.of(">=", "<=", ">", "<", "=")) {
            if (!comparison.startsWith(operator)) {
                continue;
            }
            String version = comparison.substring(operator.length());
            if (version.isEmpty()) {
                throw new IllegalArgumentException("版本比较式缺少版本 " + comparison);
            }
            int order = actual.compareTo(new DefaultArtifactVersion(version));
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
