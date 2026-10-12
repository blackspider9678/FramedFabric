# Minecraft 26.2 branch

This branch targets Minecraft **26.2**, Java 25, and Fabric Loader 0.19.5.
Fabric API and JEI versions are pinned in `gradle.properties`. The published mod
metadata restricts loading to its compiled Minecraft version.

## Build and verify

With `JAVA_HOME` set to a Java 25 JDK, run:

```powershell
.\gradlew.bat runDatagen
.\gradlew.bat build
.\gradlew.bat runClientGameTest
```

The client test launches Minecraft and closes it when finished. Its test mod lives
in `src/gametest` and is excluded from the distributable mod JAR. The mod JAR is
written to `build/libs`; the gallery screenshot is written below
`build/run/clientGameTest/screenshots`.

## Rendering regression coverage

The client test checks 396,432 combinations: every registered framed block state,
six camo materials, and six rotations. Materials cover stone, grass tinting,
translucent glass, cutout leaves, animated magma, and oak logs wrapped in a
Fabric-only model whose vanilla `collectParts` deliberately returns no geometry.
It checks quad counts, missing or neighboring textures, finite geometry/UVs,
sprite bounds, material flags, and the log's distinct side/end textures.
It also checks clicks at two heights on both door halves, server-to-client camo
synchronization, and renders a gallery in an integrated-server world.

Camo textures are sampled through Fabric's quad emission API. UV rectangles on
geometry extending outside the block are fitted inside the camo sprite before
Fabric computes transparency. This prevents atlas bleed and out-of-bounds
transparency failures. Each door half stores camo in its own rendered slot.

The automated renderer checks use Fabric's bundled Indigo renderer. Additional
renderers, shaders, resource packs, and mod combinations need their own testing.
