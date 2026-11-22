package com.modularmclib.mixinweaver.core.mixin;

import com.modularmclib.mixinweaver.core.target.GeneratedMixin;

import org.spongepowered.asm.mixin.refmap.IReferenceMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GeneratedRefmap implements IReferenceMapper {

    private final IReferenceMapper delegate;
    private final Map<String, String> origins = new HashMap<>();

    GeneratedRefmap(IReferenceMapper delegate) {
        this.delegate = delegate;
    }

    void add(List<GeneratedMixin> generated) {
        generated.forEach(mixin -> {
            origins.put(mixin.name(), mixin.originalName());
            origins.put(mixin.name().replace('.', '/'), mixin.originalName().replace('.', '/'));
        });
    }

    @Override
    public boolean isDefault() {
        return delegate.isDefault();
    }

    @Override
    public String getResourceName() {
        return delegate.getResourceName();
    }

    @Override
    public String getStatus() {
        return delegate.getStatus();
    }

    @Override
    public String getContext() {
        return delegate.getContext();
    }

    @Override
    public void setContext(String context) {
        delegate.setContext(context);
    }

    @Override
    public String remap(String className, String reference) {
        return delegate.remap(origins.getOrDefault(className, className), reference);
    }

    @Override
    public String remapWithContext(String context, String className, String reference) {
        return delegate.remapWithContext(context, origins.getOrDefault(className, className), reference);
    }
}
