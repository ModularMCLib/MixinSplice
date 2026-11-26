package com.modularmc.mixinsplice.core.extension;

import com.modularmc.mixinsplice.api.extension.SpliceExtension;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtensionRegistryTest {

    @Test
    void honoursConstraintsAndOrdersIndependentExtensionsById() {
        var registry = new ExtensionRegistry(List.of(
                new Extension("last", Set.of(), Set.of("middle")),
                new Extension("middle", Set.of(), Set.of()),
                new Extension("first", Set.of("middle", "optional"), Set.of())));

        assertEquals(List.of("first", "middle", "last"), registry.ordered().stream().map(SpliceExtension::id).toList());
    }

    @Test
    void rejectsDuplicateIdsAndCycles() {
        var first = new Extension("first", Set.of("second"), Set.of());
        var second = new Extension("second", Set.of("first"), Set.of());
        assertThrows(IllegalArgumentException.class, () -> new ExtensionRegistry(List.of(first, first)));
        assertThrows(IllegalArgumentException.class, () -> new ExtensionRegistry(List.of(first, second)));
    }

    private record Extension(String id, Set<String> before, Set<String> after) implements SpliceExtension {}
}
