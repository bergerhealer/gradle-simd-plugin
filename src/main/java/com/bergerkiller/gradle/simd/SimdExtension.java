package com.bergerkiller.gradle.simd;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class SimdExtension {
    @Inject
    public SimdExtension(ObjectFactory objects) {
        sourceSetName = objects.property(String.class);
        releaseVersion = objects.property(Integer.class);
        sourceDir = objects.directoryProperty();
        outputDir = objects.directoryProperty();
        includeInShadowJar = objects.property(Boolean.class);
    }

    public final Property<String> sourceSetName;
    public final Property<Integer> releaseVersion;
    public final DirectoryProperty sourceDir;
    public final DirectoryProperty outputDir;
    public final Property<Boolean> includeInShadowJar;

    public Property<String> getSourceSetName() {
        return sourceSetName;
    }

    public Property<Integer> getReleaseVersion() {
        return releaseVersion;
    }

    public DirectoryProperty getSourceDir() {
        return sourceDir;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public Property<Boolean> getIncludeInShadowJar() {
        return includeInShadowJar;
    }
}
