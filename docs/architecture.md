# MixinWeaver 架构

MixinWeaver 是 SpongePowered Mixin 的扩展。动态目标、跨 Mixin 注入和条件选择共用扩展注册、执行顺序与生命周期接入。

## 模块边界

| Gradle 模块 | 职责 |
| --- | --- |
| `:api` | 条件、目标、Handler、取消和注解变换的公共契约，不依赖 Loader 或 ASM |
| `:core` | Mixin 0.8.7 接入、扩展排序、条件求值、目标分析和注解处理 |
| `:loader:neoforge` | NeoForge 平台环境、模组入口、开发运行、GameTest 和发行包 |
| `:loader:fabric`、`:loader:forge` | Gradle 平台模块定义 |
| `:tests` | 条件求值和扩展排序的 JVM 测试 |

依赖方向为 `Loader → Core → API`。Loader 提供 Mod 列表、版本查询和可扫描的类路径，NeoForge 发行包收录 API 和 Core。Core 的 `mixin` 包负责与 Mixin 生命周期对接。`MixinInternals` 访问 Mixin 0.8.7 的可变 Mixin 集合、定义字节码和配置准备入口。公开的目标上下文只提供目标字节码，`IMixinInfo.getClassNode()` 返回副本，因此取消、调整和生成目标通过这些内部状态实现。Mixin 版本更新以发行环境对这些访问点和执行时机的验证为准。

## 生命周期

| 阶段 | 执行内容 |
| --- | --- |
| `IMixinConfigPlugin.onLoad()` | MixinWeaver 自带的配置发现 `WeaverExtension`，安装 `IExtension` 和 Handler 选择器 |
| `shouldApplyMixin()`、`acceptTargets()` | 求值条件，从配置移除不满足条件的目标，按需扫描类路径并注册动态目标副本 |
| `IExtension.preApply()` | 对当前目标执行取消，应用程序化注解变换和声明式注入调整，暂时移除 MixinWeaver 的类级配置注解 |
| `IExtension.postApply()` | 恢复当前目标使用的临时注入器注解及类级配置注解 |

`acceptTargets()` 处理各配置此时已准备好的 Mixin，包括使用自身配置插件的 Mixin。配置插件通过 `getMixins()` 在这一回调之后追加的条目处在自动条件筛选和动态目标扩展的扫描范围之外。

## 条件选择

`@Conditional` 的 `require` 必须全部满足，`conflict` 中任一条件满足即拒绝应用。`@ConditionSpec("example_mod")` 检查 Mod 是否存在。指定 `version` 后还检查版本。NeoForge 接受比较式（如 `>=21.1 <22`）和 Maven 版本区间。`Kind.MIXIN` 检查指定 Mixin 的字节码是否存在及其自身条件是否满足。`Kind.CUSTOM` 引用 `WeaverExtension` 注册的条件。

环境无法求值或条件出现循环时可能返回 `UNKNOWN`，按 `UnknownPolicy` 处理。默认 `DENY`，也可选择 `ALLOW` 或 `FAIL`。

## Handler、取消和调整

- `@TargetHandler` 写在注入器方法上。`@mixinweaver:handler` 根据另一 Mixin 的二进制类名和原始方法名匹配，可附加 JVM 方法描述符、Mixin 配置名和注入器前缀。
- `@CancelMixins` 写在类上，按二进制类名取消同一目标类上的 Mixin。取消集合取自条件筛选与程序化取消后的声明者，再一次性移除匹配项。条件拒绝声明者时，其注解不会贡献取消规则。
- `@AdjustInjection` 写在类上，指定收到调整的 Mixin、注入器处理方法和新的 `method` 选择器。处理方法必须唯一，重载时可附加 JVM 描述符。新选择器替换原有 `method` 和 `target`，由被调整 Mixin 的 Refmap 解析。匹配不到对应 Mixin 的目标类会跳过该声明。

动态目标副本沿用原 Mixin 的二进制类名做上述匹配，Refmap 也按原 Mixin 的键值重映射。程序化 `AnnotationTransformation` 对每个 Mixin 定义执行一次，其结果用于该定义的所有目标。声明式调整在这些变换之后执行，只作用于当前目标。多条调整按 Mixin 应用顺序处理，同一类的重复注解按声明顺序处理。同一注入器以最后一条为准。

## 动态目标

`@DynamicTargets` 使用已声明的目标作为搜索起点。`SUBCLASSES` 选择发现的子类，`IMPLEMENTERS` 选择接口实现类，`OVERRIDING` 选择覆盖相关方法的类。默认 `BYPASSING` 选择可能绕过父实现的覆盖者。若某个方法仅在部分路径调用 `super`，这些路径可能先触发父类注入，再触发覆盖者上的注入。

`packages` 限定候选目标的包名前缀，`provider` 引用已注册的 `DynamicTargetProvider`。`relaxRequirements` 只放宽生成副本的缺失注入点要求。NeoForge 从已发现的 Mod 文件根路径建立类索引，生成目标前检查目标是否已经加载。

目标分析在声明式注入调整之前执行，因此生成目标的范围取决于原始选择器和策略。

## 程序化扩展

通过 `META-INF/services/com.modularmclib.mixinweaver.api.extension.WeaverExtension` 注册提供者。`ExtensionRegistry` 使用 `before` 和 `after` 排序，排序规则对已发现的扩展 ID 生效，重复 ID 或循环依赖会在初始化时报错。

`MixinCanceller` 按目标判断是否取消 Mixin。`AnnotationTransformation` 提供类或方法的只读注解视图，可通过返回新的列表添加、修改、替换和删除注解。自定义条件和目标提供者以注册 ID 从声明式注解引用。

## 验证范围

JVM 测试验证条件求值和扩展排序。NeoForge GameTest 验证条件允许与拒绝、跨 Mixin 注入、注解作用域、动态副本匹配、方法覆盖、缺失注入点及接口实现。当前组合为 Minecraft 1.21.1、NeoForge 21.1.265、Mixin 0.8.7 和 Java 21。

## 上游参考

MixinWeaver 按现有模块和公开契约重新实现以下能力。

- MixinSquared 的 Handler 定位、取消、注解调整和排序。MIT，快照 `f9cf62f`。[参考提交](https://github.com/Bawnorton/MixinSquared/tree/f9cf62fc3f41c18ff28a72df1cbdd2a78ba4809c)
- conditional-mixin 的条件选择和版本谓词。LGPL-3.0，快照 `be560b0`。[参考提交](https://github.com/Fallen-Breath/conditional-mixin/tree/be560b03ebe94295ad496e75928e25ac9d15f8f0)
- MixinScope（PolyMixin）的动态目标、继承分析与 `super` 控制流。MIT，快照 `81af058`。[参考提交](https://github.com/Dust-Pile/polymixin/tree/81af05835f55c548581de30eb8400951f39066ec)
