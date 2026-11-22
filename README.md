# MixinWeaver

[简体中文](#简体中文) · [English](#english)

## 简体中文

MixinWeaver 是 SpongePowered Mixin 的扩展，提供条件选择、动态目标、跨 Mixin 注入以及声明式取消和注入调整。

### 使用方式

在 NeoForge 开发环境中引入 MixinWeaver 后，将 Mixin 注册到原有的 `mixins.json`，再按需要添加注解。MixinWeaver 的模组负责安装配置插件和转换扩展。

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

- `@Conditional` 支持 Mod、版本、Mixin 和自定义条件。`@ConditionSpec("example_mod")` 默认检查 Mod 是否存在。
- `@CancelMixins` 在当前目标类上取消指定 Mixin。`@AdjustInjection` 在两个 Mixin 共同作用的目标类上替换注入选择器。引用其他 Mixin 时使用二进制类名，例如 `example.other.Outer$NestedMixin`。
- `@DynamicTargets` 默认扩展到可以绕过父实现的覆盖者，也可选择子类、所有覆盖者、接口实现者或自定义目标。
- `@TargetHandler` 配合注入器的 `method = "@mixinweaver:handler"` 选择同一个目标类中另一个 Mixin 的处理方法。
- `WeaverExtension` 通过 `ServiceLoader` 提供需要 Java 逻辑的规则，包括自定义条件、目标提供者、取消器和注解变换。

注解的作用域、执行顺序和限制见[架构文档](docs/architecture.md)。

### 构建

当前运行平台为 NeoForge，目标 Minecraft 1.21.1、Mixin 0.8.7，使用 Java 21。通过环境变量 `GRADLE_USER_HOME` 指定 Gradle 缓存目录。

```powershell
.\gradlew.bat build
.\gradlew.bat :loader:neoforge:slimJar
.\gradlew.bat :loader:neoforge:runGameTestServer
```

主 JAR、源码 JAR 和 slim JAR 位于根项目的 `build/libs/`。主 JAR 的名称形如 `MixinWeaver-neoforge--1.21.1-0.0.3.jar`。

## English

MixinWeaver is an extension for SpongePowered Mixin. It provides conditional selection, dynamic targets, cross-Mixin injection, declarative cancellation, and injection adjustments.

### Usage

Add MixinWeaver to your NeoForge development environment, keep your Mixin in its regular `mixins.json`, and add the annotations you need. The MixinWeaver mod installs the config plugin and transformation extension.

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

- `@Conditional` supports mod presence, versions, Mixin presence, and custom conditions. `@ConditionSpec("example_mod")` checks mod presence by default.
- `@CancelMixins` cancels named Mixins on the current target class. `@AdjustInjection` replaces an injection selector on a target shared by both Mixins. Refer to other Mixins by binary class name, such as `example.other.Outer$NestedMixin`.
- `@DynamicTargets` defaults to overrides that can bypass the parent implementation. It also supports subclasses, all overrides, interface implementers, and custom targets.
- `@TargetHandler` works with an injector's `method = "@mixinweaver:handler"` to select a handler from another Mixin on the same target class.
- `WeaverExtension` uses `ServiceLoader` to supply Java rules for custom conditions, target providers, cancellation, and annotation transformations.

For annotation scope, execution order, and limitations, see the [architecture document](docs/architecture.md).

### Build

The current platform is NeoForge, targeting Minecraft 1.21.1 and Mixin 0.8.7 with Java 21. Set `GRADLE_USER_HOME` to select the Gradle cache directory.

```powershell
.\gradlew.bat build
.\gradlew.bat :loader:neoforge:slimJar
.\gradlew.bat :loader:neoforge:runGameTestServer
```

The main, sources, and slim JARs are written to the root project's `build/libs/` directory. The main JAR is named like `MixinWeaver-neoforge--1.21.1-0.0.3.jar`.
