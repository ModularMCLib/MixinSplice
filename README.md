# MixinSplice

[简体中文](#简体中文) · [English](#english)

## 简体中文

MixinSplice 是 SpongePowered Mixin 的扩展，提供条件选择、动态目标、跨 Mixin 注入以及声明式取消和注入调整。

### 使用方式

安装时选择 Fabric、Forge 或 NeoForge 对应的主 JAR。主 JAR 已内嵌共享库，并通过自己的 `mixinsplice.mixins.json` 启动扩展。消费方在自己的 `mixins.json` 中注册 Mixin，再按需要添加注解。

```java
@Conditional(require = @ConditionSpec("example_mod"))
@CancelMixins("example.other.LegacyMixin")
@AdjustInjection(
        mixin = "example.other.BlockMixin",
        handler = "onTick",
        method = "scheduledTick"
)
@Mixin(TargetBlock.class)
abstract class CompatibilityMixin {}
```

- `@Conditional` 在 Mixin 解析目标类前检查 Mod、版本、Mixin 和自定义条件。`@ConditionSpec("example_mod")` 检查 Mod 是否存在。`versions = {"1.21.x", ">=1.22"}` 表示任一版本条件成立即可。自定义条件可直接写 `@ConditionSpec(tester = MyCondition.class)`。
- `@CancelMixins` 在当前目标类上取消指定 Mixin。`@AdjustInjection` 在两个 Mixin 共同作用的目标类上调整方法、`@At`、`@Slice`、序号或注入次数，也可移除注入器。引用其他 Mixin 时使用二进制类名，例如 `example.other.Outer$NestedMixin`。
- `@DynamicTargets` 默认扩展到可以绕过父实现的覆盖者，也可选择子类、所有覆盖者、接口实现者或自定义目标。选择和副本生成均处理 MixinExtras 注入器。迁移依赖 PolyMixin 默认宽松行为的 Mixin 时，填写 `relaxRequirements = true`。
- `@TargetHandler` 配合注入器的 `method = "@mixinsplice:handler"` 选择同一个目标类中另一个 Mixin 的处理方法。调试时可用 `print = true` 查看候选，并将该注入器设为 `require = 0, expect = 0`。
- `SpliceExtension` 通过 `ServiceLoader` 提供需要 Java 逻辑的规则，包括自定义条件、目标提供者、取消器和注解变换。

### Gradle 依赖

Maven 分组为 `com.modularmclib.mixinsplice`。编译时使用 `mixinsplice-common`。使用 Mixin 注解处理器为 `@TargetHandler` 生成 Refmap 时，再把同一个制品加入 `annotationProcessor`。其中的 javac 插件会在 Mixin 注解处理开始前注册 Handler 选择器，并沿用项目的 Mixin 映射配置。

```groovy
dependencies {
    compileOnly "com.modularmclib.mixinsplice:mixinsplice-common:${mixinsplice_version}"
    annotationProcessor "com.modularmclib.mixinsplice:mixinsplice-common:${mixinsplice_version}"
}
```

内嵌发行时，将对应的平台制品加入消费方的打包配置。开发运行也需加载同一个平台制品。

| Loader | Maven 制品 | 内嵌方式 |
| --- | --- | --- |
| Fabric | `mixinsplice-fabric` | Loom 的 `include` |
| Forge | `mixinsplice-forge` | `dependencies` 中的 `jarJar` |
| NeoForge | `mixinsplice-neoforge` | `dependencies` 中的 `jarJar` |

使用 ForgeGradle 6 的项目需调用 `jarJar.enable()`，并分发 `jarJar` 任务生成的 JAR。

注解的作用域、执行顺序和限制见[架构文档](docs/architecture.md)。

### 平台版本

| Loader | 元数据声明的 Minecraft 下限 | Loader 版本要求 |
| --- | --- | --- |
| Fabric | 1.21 | Fabric Loader 0.16.0 及以上 |
| Forge | 1.21 | Forge 51.0.33 至 61.x |
| NeoForge | 1.21 | NeoForge 21.1.26 及以上 |

三个发行包以 Java 21 和 Mixin 0.8.7 为基线。Minecraft 1.21 对应的 NeoForge 21.0 系列最高到 21.0.167，内置 Mixin 0.8.6。满足 Mixin 要求的首个 NeoForge 版本是 21.1.26，对应 Minecraft 1.21.1。Forge 62 对应 Minecraft 26.1，Mod 列表查询接口从该版本起改动。三平台的运行测试以 Minecraft 1.21.1 为基线。

### 构建

用 Java 21 运行 Gradle Wrapper。通过环境变量 `GRADLE_USER_HOME` 指定 Gradle 缓存目录。三平台的主 JAR 由 NeoGradle 的 `jarJar` 任务生成。

```powershell
.\gradlew.bat build
.\gradlew.bat :loader:fabric:slimJar :loader:forge:slimJar :loader:neoforge:slimJar
.\gradlew.bat :loader:neoforge:runGameTestServer
.\gradlew.bat publishToMavenLocal
```

共享库、平台主 JAR、源码 JAR 和单独构建的 slim JAR 位于根项目的 `build/libs/`。通过 `publishToMavenLocal` 安装制品后，消费方可使用 `mavenLocal()` 解析依赖。

## English

MixinSplice is an extension for SpongePowered Mixin. It provides conditional selection, dynamic targets, cross-Mixin injection, declarative cancellation, and injection adjustments.

### Usage

Install the main JAR for Fabric, Forge, or NeoForge. It embeds the shared library and activates the extension through its own `mixinsplice.mixins.json`. Register your Mixins in your mod's `mixins.json` and add the annotations you need.

```java
@Conditional(require = @ConditionSpec("example_mod"))
@CancelMixins("example.other.LegacyMixin")
@AdjustInjection(
        mixin = "example.other.BlockMixin",
        handler = "onTick",
        method = "scheduledTick"
)
@Mixin(TargetBlock.class)
abstract class CompatibilityMixin {}
```

- `@Conditional` checks mod presence, versions, Mixin presence, and custom conditions before Mixin resolves target classes. `@ConditionSpec("example_mod")` checks mod presence. `versions = {"1.21.x", ">=1.22"}` accepts either version predicate. Use `@ConditionSpec(tester = MyCondition.class)` to name a custom condition directly.
- `@CancelMixins` cancels named Mixins on the current target class. `@AdjustInjection` can change methods, `@At`, `@Slice`, ordinals, or counts on an injector, or remove the injector on a shared target. Refer to other Mixins by binary class name, such as `example.other.Outer$NestedMixin`.
- `@DynamicTargets` defaults to overrides that can bypass the parent implementation. It also supports subclasses, all overrides, interface implementers, and custom targets. Target selection and generated copies account for MixinExtras injectors. Set `relaxRequirements = true` when migrating a Mixin that relies on PolyMixin's relaxed defaults.
- `@TargetHandler` works with an injector's `method = "@mixinsplice:handler"` to select a handler from another Mixin on the same target class. Use `print = true` with `require = 0, expect = 0` to list candidates.
- `SpliceExtension` uses `ServiceLoader` to supply Java rules for custom conditions, target providers, cancellation, and annotation transformations.

### Gradle dependencies

The Maven group is `com.modularmclib.mixinsplice`. Use `mixinsplice-common` for compilation. If you use the Mixin annotation processor to generate a Refmap for `@TargetHandler`, also add the same artifact to `annotationProcessor`. Its javac plugin registers the handler selector before Mixin annotation processing and uses your project's existing Mixin mappings.

```groovy
dependencies {
    compileOnly "com.modularmclib.mixinsplice:mixinsplice-common:${mixinsplice_version}"
    annotationProcessor "com.modularmclib.mixinsplice:mixinsplice-common:${mixinsplice_version}"
}
```

To embed MixinSplice in your mod, add the platform artifact to your build's packaging configuration. Load the same platform artifact in development runs.

| Loader | Maven artifact | Packaging configuration |
| --- | --- | --- |
| Fabric | `mixinsplice-fabric` | Loom's `include` |
| Forge | `mixinsplice-forge` | `jarJar` in `dependencies` |
| NeoForge | `mixinsplice-neoforge` | `jarJar` in `dependencies` |

With ForgeGradle 6, call `jarJar.enable()` and distribute the JAR from the `jarJar` task.

For annotation scope, execution order, and limitations, see the [architecture document](docs/architecture.md).

### Platform versions

| Loader | Minimum Minecraft version in metadata | Loader requirement |
| --- | --- | --- |
| Fabric | 1.21 | Fabric Loader 0.16.0 or newer |
| Forge | 1.21 | Forge 51.0.33 through 61.x |
| NeoForge | 1.21 | NeoForge 21.1.26 or newer |

All three distributions target Java 21 and Mixin 0.8.7. NeoForge 21.0 for Minecraft 1.21 ends at 21.0.167, which bundles Mixin 0.8.6. NeoForge 21.1.26 for Minecraft 1.21.1 is the first release that meets the Mixin requirement. Forge 62 targets Minecraft 26.1 and changes the mod-list API. Runtime tests on all three platforms use Minecraft 1.21.1.

### Build

Use Java 21 to run the Gradle Wrapper. Set `GRADLE_USER_HOME` to select the Gradle cache directory. NeoGradle's `jarJar` task builds the main JAR on all three platforms.

```powershell
.\gradlew.bat build
.\gradlew.bat :loader:fabric:slimJar :loader:forge:slimJar :loader:neoforge:slimJar
.\gradlew.bat :loader:neoforge:runGameTestServer
.\gradlew.bat publishToMavenLocal
```

The shared library, platform JARs, sources, and separately built slim JARs are written to the root project's `build/libs/` directory. After running `publishToMavenLocal`, consumer builds can resolve the artifacts from `mavenLocal()`.
