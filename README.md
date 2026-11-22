# MixinWeaver

MixinWeaver is a unified extension framework for SpongePowered Mixin.
See the [architecture document](docs/architecture.md) for the module layout,
Mixin lifecycle, and upstream source review checklist.

## Build

Use Java 21 and the Gradle Wrapper. Set `GRADLE_USER_HOME` in the environment
to select the Gradle cache location.

```powershell
.\gradlew.bat projects
.\gradlew.bat build
.\gradlew.bat :distribution:neoforge:slimJar
```

The NeoForge distribution writes its main and source JARs to `build/libs/`.
The slim JAR is built by the separate task above.
NeoForge development tasks, including `runClient` and `runGameTestServer`,
are defined in `:loader:neoforge`.
The current GameTest server reaches mod discovery and reports
`No test functions were given!` until GameTest cases are added.
