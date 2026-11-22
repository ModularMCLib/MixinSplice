# MixinWeaver

[简体中文](#简体中文) · [English](#english)

## 简体中文

MixinWeaver 是 SpongePowered Mixin 的扩展，计划提供动态目标发现、跨 Mixin 注入和条件式 Mixin 应用。[架构文档](docs/architecture.md)记录了模块划分、Mixin 生命周期和上游源码审查清单。

### 构建

使用 Java 21 和项目的 Gradle Wrapper。通过环境变量 `GRADLE_USER_HOME` 指定 Gradle 缓存目录。

```powershell
.\gradlew.bat projects
.\gradlew.bat build
.\gradlew.bat :loader:neoforge:slimJar
```

NeoForge 的模组入口、开发运行和发行打包位于 `:loader:neoforge`，包括 `runClient` 和 `runGameTestServer`。
主 JAR 和源码 JAR 输出到 `build/libs/`，上面的独立任务生成 slim JAR。
主 JAR 的文件名形如 `MixinWeaver-neoforge--1.21.1-0.0.3.jar`。
当前 GameTest 服务端可以发现模组，随后因没有测试用例报告 `No test functions were given!`。

## English

MixinWeaver is an extension for SpongePowered Mixin. Its planned features
include dynamic target discovery, cross-Mixin injection, and conditional
Mixin application.
The module layout, Mixin lifecycle, and upstream source review checklist are
described in the [architecture document](docs/architecture.md).

### Build

Use Java 21 and the Gradle Wrapper. Set `GRADLE_USER_HOME` in the environment
to select the Gradle cache location.

```powershell
.\gradlew.bat projects
.\gradlew.bat build
.\gradlew.bat :loader:neoforge:slimJar
```

`:loader:neoforge` contains the mod entry points, development runs such as
`runClient` and `runGameTestServer`, and release packaging.
The main and sources JARs go to `build/libs/`. The separate task above builds
the slim JAR. A main JAR is named `MixinWeaver-neoforge--1.21.1-0.0.3.jar`.
The current GameTest server reaches mod discovery and reports
`No test functions were given!` until GameTest cases are added.
