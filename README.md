# MixinWeaver

[简体中文](#简体中文) · [English](#english)

## 简体中文

MixinWeaver 是基于 SpongePowered Mixin 的统一扩展框架。模块划分、Mixin 生命周期和上游源码审查清单见[架构文档](docs/architecture.md)。

### 构建

使用 Java 21 和项目的 Gradle Wrapper。通过环境变量 `GRADLE_USER_HOME` 指定 Gradle 缓存目录。

```powershell
.\gradlew.bat projects
.\gradlew.bat build
.\gradlew.bat :distribution:neoforge:slimJar
```

NeoForge 的主 JAR 和源码 JAR 输出到 `build/libs/`。上面的独立任务生成 slim JAR。
`runClient` 和 `runGameTestServer` 等开发任务由 `:loader:neoforge` 提供。
当前 GameTest 服务端可以发现模组，随后因没有测试用例报告 `No test functions were given!`。

## English

MixinWeaver is a unified extension framework for SpongePowered Mixin.
The module layout, Mixin lifecycle, and upstream source review checklist are
described in the [architecture document](docs/architecture.md).

### Build

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
