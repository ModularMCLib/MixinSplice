# MixinWeaver

[简体中文](#简体中文) · [English](#english)

## 简体中文

MixinWeaver 是 SpongePowered Mixin 的扩展，提供条件选择、动态目标、跨 Mixin 注入以及声明式取消和注入调整。

### 使用方式

选择 Fabric、Forge 或 NeoForge 对应的主 JAR。将 Mixin 注册到原有的 `mixins.json`，再按需要添加注解。MixinWeaver 的配置会自动注册转换扩展。

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

- `@Conditional` 支持 Mod、版本、Mixin 和自定义条件。`@ConditionSpec("example_mod")` 检查 Mod 是否存在。`versions = {"1.21.x", ">=1.22"}` 表示任一版本条件成立即可。
- `@CancelMixins` 在当前目标类上取消指定 Mixin。`@AdjustInjection` 在两个 Mixin 共同作用的目标类上替换注入选择器。引用其他 Mixin 时使用二进制类名，例如 `example.other.Outer$NestedMixin`。
- `@DynamicTargets` 默认扩展到可以绕过父实现的覆盖者，也可选择子类、所有覆盖者、接口实现者或自定义目标。
- `@TargetHandler` 配合注入器的 `method = "@mixinweaver:handler"` 选择同一个目标类中另一个 Mixin 的处理方法。
- `WeaverExtension` 通过 `ServiceLoader` 提供需要 Java 逻辑的规则，包括自定义条件、目标提供者、取消器和注解变换。

注解的作用域、执行顺序和限制见[架构文档](docs/architecture.md)。

### 平台版本

| Loader | 元数据声明的 Minecraft 下限 | Loader 版本要求 |
| --- | --- | --- |
| Fabric | 1.21 | Fabric Loader 0.16.0 及以上 |
| Forge | 1.21 | Forge 51.0.33 至 61.x |
| NeoForge | 1.21 | NeoForge 21.1.26 及以上 |

三个发行包以 Java 21 和 Mixin 0.8.7 为基线。Minecraft 1.21 对应的 NeoForge 21.0 系列最高到 21.0.167，内置 Mixin 0.8.6。满足 Mixin 要求的首个 NeoForge 版本是 21.1.26，对应 Minecraft 1.21.1。Forge 62 对应 Minecraft 26.1，Mod 列表查询接口从该版本起改动。三平台的运行测试以 Minecraft 1.21.1 为基线。

### 构建

用 Java 21 运行 Gradle Wrapper。通过环境变量 `GRADLE_USER_HOME` 指定 Gradle 缓存目录。NeoForge 开发运行使用 NeoGradle。

```powershell
.\gradlew.bat build
.\gradlew.bat :loader:fabric:slimJar :loader:forge:slimJar :loader:neoforge:slimJar
.\gradlew.bat :loader:neoforge:runGameTestServer
```

主 JAR、源码 JAR 和 slim JAR 位于根项目的 `build/libs/`。主 JAR 的命名格式为 `MixinWeaver-<loader>--<minecraft>-<version>.jar`。

## English

MixinWeaver is an extension for SpongePowered Mixin. It provides conditional selection, dynamic targets, cross-Mixin injection, declarative cancellation, and injection adjustments.

### Usage

Choose the main JAR for Fabric, Forge, or NeoForge. Keep your Mixin in its regular `mixins.json` and add the annotations you need. MixinWeaver's config registers the transformation extension automatically.

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

- `@Conditional` supports mod presence, versions, Mixin presence, and custom conditions. `@ConditionSpec("example_mod")` checks mod presence. `versions = {"1.21.x", ">=1.22"}` accepts either version predicate.
- `@CancelMixins` cancels named Mixins on the current target class. `@AdjustInjection` replaces an injection selector on a target shared by both Mixins. Refer to other Mixins by binary class name, such as `example.other.Outer$NestedMixin`.
- `@DynamicTargets` defaults to overrides that can bypass the parent implementation. It also supports subclasses, all overrides, interface implementers, and custom targets.
- `@TargetHandler` works with an injector's `method = "@mixinweaver:handler"` to select a handler from another Mixin on the same target class.
- `WeaverExtension` uses `ServiceLoader` to supply Java rules for custom conditions, target providers, cancellation, and annotation transformations.

For annotation scope, execution order, and limitations, see the [architecture document](docs/architecture.md).

### Platform versions

| Loader | Minimum Minecraft version in metadata | Loader requirement |
| --- | --- | --- |
| Fabric | 1.21 | Fabric Loader 0.16.0 or newer |
| Forge | 1.21 | Forge 51.0.33 through 61.x |
| NeoForge | 1.21 | NeoForge 21.1.26 or newer |

All three distributions target Java 21 and Mixin 0.8.7. NeoForge 21.0 for Minecraft 1.21 ends at 21.0.167, which bundles Mixin 0.8.6. NeoForge 21.1.26 for Minecraft 1.21.1 is the first release that meets the Mixin requirement. Forge 62 targets Minecraft 26.1 and changes the mod-list API. Runtime tests on all three platforms use Minecraft 1.21.1.

### Build

Use Java 21 to run the Gradle Wrapper. Set `GRADLE_USER_HOME` to select the Gradle cache directory. NeoForge development runs use NeoGradle.

```powershell
.\gradlew.bat build
.\gradlew.bat :loader:fabric:slimJar :loader:forge:slimJar :loader:neoforge:slimJar
.\gradlew.bat :loader:neoforge:runGameTestServer
```

The main, sources, and slim JARs are written to the root project's `build/libs/` directory. Main JARs follow `MixinWeaver-<loader>--<minecraft>-<version>.jar`.
