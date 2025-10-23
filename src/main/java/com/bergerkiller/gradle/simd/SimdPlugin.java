package com.bergerkiller.gradle.simd;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

public class SimdPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        SimdExtension extension = project.getExtensions()
                .create("simd", SimdExtension.class, project.getObjects());

        extension.getSourceSetName().convention("simd");
        extension.getReleaseVersion().convention(17);
        extension.getSourceDir().convention(
                project.getLayout().getProjectDirectory().dir("src/simd/java")
        );
        extension.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("classes/java/simd")
        );
        extension.getIncludeInShadowJar().convention(true);

        // Ensure Java plugin is applied
        project.getPlugins().apply("java");

        SourceSetContainer sourceSets = project.getExtensions()
                .getByType(JavaPluginExtension.class)
                .getSourceSets();

        project.afterEvaluate(p -> {
            extension.getSourceSetName().finalizeValue();
            extension.getReleaseVersion().finalizeValue();
            extension.getSourceDir().finalizeValue();
            extension.getOutputDir().finalizeValue();
            extension.getIncludeInShadowJar().finalizeValue();

            // Register SIMD compile task
            TaskProvider<JavaCompile> compileSimd = project.getTasks().register("compileSimd", JavaCompile.class, task -> {
                int release = extension.getReleaseVersion().get();
                if (release < 17) {
                    throw new GradleException("simd.releaseVersion must be 17 or higher. Got: " + release);
                }

                File simdDir = extension.getSourceDir().get().getAsFile();
                if (!simdDir.exists()) {
                    project.getLogger().warn("SIMD source directory does not exist: {}", simdDir);
                }
                task.setSource(simdDir);

                SourceSet main = project.getExtensions()
                        .getByType(JavaPluginExtension.class)
                        .getSourceSets()
                        .getByName("main");

                task.setClasspath(main.getCompileClasspath().plus(main.getOutput()));
                task.getDestinationDirectory().set(extension.getOutputDir());
                task.getOptions().getCompilerArgs().addAll(Arrays.asList(
                        "--release", Integer.toString(release),
                        "--add-modules=jdk.incubator.vector")
                );
            });

            // Hook into test
            project.getTasks().named("test", Test.class).configure(test -> {
                test.dependsOn(compileSimd);
                test.setJvmArgs(Collections.singletonList("--add-modules=jdk.incubator.vector"));
                test.setClasspath(test.getClasspath().plus(
                        project.files(compileSimd.get().getDestinationDirectory())
                ));
            });

            // Hook into javadoc
            project.getTasks().named("javadoc", Javadoc.class).configure(javadoc -> {
                javadoc.dependsOn(compileSimd);
            });

            // Hook into jar
            project.getTasks().named("jar", Jar.class).configure(jar -> {
                jar.dependsOn(compileSimd);
                jar.from(project.fileTree(compileSimd.get().getDestinationDirectory().get().getAsFile()));
            });

            // Also update shadowJar, which normally only includes the "main" classes set
            // We cannot put our simd classes along in the same set as that would cause weird problems
            // This could technically also be done by the projects that use this plugin
            if (extension.getIncludeInShadowJar().get()) {
                project.getTasks().named("shadowJar").configure(shadowJar -> {
                    shadowJar.dependsOn(compileSimd);

                    Provider<FileTree> simdClassTree = extension.getOutputDir().map(dir ->
                            project.fileTree(dir.getAsFile()).matching(pattern -> pattern.include("**/*.class"))
                    );

                    try {
                        Method fromMethod = shadowJar.getClass().getMethod("from", Object[].class);
                        fromMethod.invoke(shadowJar, (Object) (new Object[] { simdClassTree }));
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new UnsupportedOperationException("Could not add simd classes to shadowJar", e);
                    }
                });
            }
        });
    }
}
