# Gradle SIMD plugin
A very simple plugin that makes it possible to compile java files that require Java 17 and the experimental vector module (`jdk.incubator.vector`), in a project that normally does not support this.

## Compiling
The files are made available as a separate .class compiled source set, which is merged into the final produced jar at the end.
That means these files are not available for the rest of the source tree, and must be loaded using reflection (Class.forName).
As such, make sure your SIMD implementations use a common interface defined in your main project.
The classes are also automatically added to the **shadowjar** plugin, which can be turned off with plugin configuration.

## Usage
### Add the plugin
#### build.gradle.kts
```kts
plugins {
    id("com.bergerkiller.gradle.simd") version "1.0.0"
}
```

#### settings.gradle.kts
```kts
pluginManagement {
    repositories {
        maven("https://ci.mg-dev.eu/plugin/repository/everything/")
    }
}
```

### Optional configuration (defaults)
```kts
simd {
    sourceSetName = "simd"
    releaseVersion = 17
    sourceDir.set(layout.projectDirectory.dir("src/simd/java"));
    outputDir.set(layout.buildDirectory.dir("classes/java/simd"));
    includeInShadowJar = true
}
```

### Add your java classes
You can put your SIMD-using classes in your simd source directory (`src/simd/java`) and they will automatically be made available for:
- Tests
- Produced jar artifacts
- Produced shadow jar artifacts

### Load your java classes at runtime
Use `Class.forName("name")` to load your classes. Here is a boilerplate example that also does at-runtime verification that SIMD is working.
<details>
  <summary>Click to show example code</summary>

  ```java
import com.bergerkiller.bukkit.common.Logging;
import com.bergerkiller.mountiplex.reflection.util.FastConstructor;
import org.bukkit.util.Vector;

import java.util.function.Supplier;
import java.util.logging.Level;

final class VertexPointsBoxBuilderSelector {
    public static final Supplier<VertexPoints.BoxBuilder> BUILDER_IMPL = selectBuilderImpl();

    @SuppressWarnings("unchecked")
    private static Supplier<VertexPoints.BoxBuilder> selectBuilderImpl() {
        // Detect that SIMD is available at all on the platform we are running
        final String simdImplementationClassName;
        try {
            // Check if Vector API is available
            Class.forName("jdk.incubator.vector.DoubleVector");

            // Get preferred species
            jdk.incubator.vector.VectorSpecies<Double> preferred = jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED;
            int lanes = preferred.length();

            String packagePath = VertexPointsBoxBuilderSelector.class.getName();
            packagePath = packagePath.substring(0, packagePath.lastIndexOf('.'));

            if (lanes >= 8) {
                simdImplementationClassName = packagePath + ".VertexPointsSIMD512Impl$BoxBuilder";
            } else if (lanes >= 4) {
                simdImplementationClassName = packagePath + ".VertexPointsSIMD256Impl$BoxBuilder";
            } else {
                throw new UnsupportedOperationException("SIMD not supported");
            }
        } catch (Throwable ignored) {
            return VertexPointsBasicImpl.BoxBuilder::new;
        }

        // Try to instantiate the suitable SIMD implementation
        try {
            ClassLoader classLoader = VertexPointsBoxBuilderSelector.class.getClassLoader();
            Class<? extends VertexPoints.BoxBuilder> boxBuilderClass = (Class<? extends VertexPoints.BoxBuilder>) Class.forName(
                    simdImplementationClassName, true, classLoader);
            final FastConstructor<? extends VertexPoints.BoxBuilder> ctor = new FastConstructor<>(boxBuilderClass.getConstructor());

            // Test it
            VertexPoints pts = ctor.newInstance()
                    .halfSize(new Vector(1, 1, 1))
                    .rotate(Quaternion.fromYawPitchRoll(45, 90, 180))
                    .translate(new Vector(10, 20, 30))
                    .build();
            if (pts == null) {
                throw new IllegalStateException("Builder returned null");
            }

            // Works - use it
            return ctor::newInstance;
        } catch (Throwable t) {
            Logging.LOGGER.log(Level.WARNING, "Failed to initialize SIMD " + simdImplementationClassName, t);
            return VertexPointsBasicImpl.BoxBuilder::new;
        }
    }
}
  ```
</details>

## License
MIT License

