# MixinSplice 架构

MixinSplice 是 SpongePowered Mixin 的扩展。动态目标、跨 Mixin 注入和条件选择共用扩展注册、执行顺序与生命周期接入。

## 模块边界

| Gradle 模块 | 职责 |
| --- | --- |
| `:api` | 条件、目标、Handler、取消和注解变换的公共契约，共用 Mixin 的注入注解类型 |
| `:core` | Mixin 0.8.7 接入、扩展排序、条件求值、目标分析和注解处理 |
| `:loader:neoforge` | NeoForge 平台环境、NeoGradle 开发运行、GameTest 和发行包 |
| `:loader:fabric` | Fabric 平台环境、Loom 发行包和生产服务端测试 |
| `:loader:forge` | Forge 平台环境、JarJar 发行包和 ForgeGradle GameTest |
| `:tests` | 条件求值和扩展排序的 JVM 测试，以及三平台共用的 Mixin 用例 |

依赖方向为 `Loader → Core → API`。各平台提供 Mod 列表、版本查询和可扫描的类路径。Core 的 `MixinInternals` 访问 Mixin 0.8.7 的可变 Mixin 集合、定义字节码和配置准备入口。公开的目标上下文只提供目标字节码，`IMixinInfo.getClassNode()` 返回副本，因此取消、调整和生成目标通过这些内部状态实现。

## Jar-in-Jar 与 Mixin 配置

`:core` 将 API、实现和 javac 插件打进 `mixinsplice-common`。三平台都引用 `:core` 的 `commonElements`。Fabric 通过 Loom 的 `include` 把共享库嵌入 `META-INF/jars/`，Loom 生成 `fabric.mod.json` 的 `jars` 声明。共享库的 Fabric ID 为 `mixinsplice_common`。Forge 通过 Forge JarJar 插件打包，NeoForge 通过 NeoGradle 打包，两者用 `jarJar` 依赖把共享库放进 `META-INF/jarjar/` 并生成元数据。共享库的 Manifest 声明 `FMLModType: GAMELIBRARY`，供 Forge 和 NeoForge 将它载入游戏层。

初始化配置 `mixinsplice.mixins.json` 随平台包分发。Fabric 在 `fabric.mod.json` 中声明 `mixins`，Forge 在 Manifest 中声明 `MixinConfigs`，NeoForge 在 `neoforge.mods.toml` 中声明 `[[mixins]]`。Forge 使用 `lowcodefml` 注册平台模组，NeoForge 使用 `javafml` 并保留 FML 版本下限。共享库提供配置中的插件类 `MixinSpliceConfigPlugin`。配置的 `package` 为 `core.mixins`，让 Mixin 将该包前缀留给 Mixin 类，插件及运行时类位于 `core.mixin`。

消费方通过自己的平台元数据注册自己的 `mixins.json`。平台包作为独立 Mod 安装或作为消费方的 Jar-in-Jar 依赖加载时，都会自动注册 MixinSplice 的初始化配置。

## 生命周期

| 阶段 | 执行内容 |
| --- | --- |
| `IMixinConfigPlugin.onLoad()` | MixinSplice 自带的配置发现 `SpliceExtension`，安装 `IExtension` 和 Handler 选择器 |
| `IExtension.checkActive()` | Mixin 已选出配置并即将读取 Mixin 定义。此时接入配置的字节码提供器，在目标解析前筛选 `@Mixin` 目标 |
| `acceptTargets()` | 按需扫描类路径，为已准备的 Mixin 注册动态目标副本 |
| `IExtension.preApply()` | 对当前目标执行取消，应用程序化注解变换和声明式注入调整，暂时移除 MixinSplice 的类级配置注解 |
| `IExtension.postApply()` | 恢复当前目标使用的临时注入器注解及类级配置注解 |

`getMixins()` 追加的条目经过同一字节码提供器，在其准备回调中补做动态目标分析。生成副本使用独立的待处理列表。MixinSplice 在读取注入器注解的扩展之前应用取消与注解调整。

## 条件选择

`@Conditional` 的 `require` 必须全部满足，`conflict` 中任一条件满足即拒绝应用。`@ConditionSpec("example_mod")` 检查 Mod 是否存在。`version` 接受单个谓词，`versions` 中任一谓词满足即可。三个平台接受比较式（如 `>=1.21 <1.22`）和数字版本通配符（如 `1.21.x`）。Fabric 使用 Loader 原生谓词，Forge 和 NeoForge 也接受 Maven 版本区间。`Kind.MIXIN` 检查指定 Mixin 的字节码是否存在及其自身条件是否满足。`Kind.CUSTOM` 引用 `SpliceExtension` 注册的条件。

环境无法求值或条件出现循环时可能返回 `UNKNOWN`，按 `UnknownPolicy` 处理。默认 `DENY`，也可选择 `ALLOW` 或 `FAIL`。

类条件通过 `@ConditionSpec(tester = MyCondition.class)` 引用。条件类实现 `Condition`，并提供公共无参构造器。求值器复用实例，按目标调用 `evaluate()`。程序化调用可以通过 `ConditionQueries.get()` 查询判断结果、接收拒绝原因、读取 Mod 版本或匹配指定版本。

## Handler、取消和调整

- `@TargetHandler` 写在注入器方法上。`@mixinsplice:handler` 根据另一 Mixin 的二进制类名和原始方法名匹配，可附加 JVM 方法描述符、Mixin 配置名和注入器前缀。`print = true` 在日志列出当前目标类上的候选，并跳过本次匹配。项目使用 Mixin 注解处理器时，MixinSplice 的 javac 插件将 Handler 描述符重映射到项目生成的 Refmap。
- `@CancelMixins` 写在类上，按二进制类名取消同一目标类上的 Mixin。取消集合取自条件筛选与程序化取消后的声明者，再一次性移除匹配项。条件拒绝声明者时，其注解不会贡献取消规则。
- `@AdjustInjection` 写在类上，指定收到调整的 Mixin 和注入器处理方法。处理方法必须唯一，重载时可附加 JVM 描述符。填写 `method` 或 `target` 会替换选择器组，填写 `at` 或 `slice` 会替换对应注解，`ordinals` 在注入点替换后按位置修改。`require`、`expect` 和 `allow` 调整计数，`remove = true` 删除注入器。接收方的 Refmap 用于解析新选择器和注入点。

动态目标副本沿用原 Mixin 的二进制类名做上述匹配，Refmap 也按原 Mixin 的键值重映射。程序化 `AnnotationTransformation` 对每个 Mixin 定义执行一次，其结果用于该定义的所有目标。声明式调整在这些变换之后执行，只作用于当前目标。多条调整按 Mixin 应用顺序处理，同一类的重复注解按声明顺序处理。多条规则修改同一属性时取最后一次设置。

## 动态目标

`@DynamicTargets` 使用通过条件筛选的声明目标作为搜索起点。`SUBCLASSES` 选择发现的子类，`IMPLEMENTERS` 选择接口实现类，`OVERRIDING` 选择覆盖相关方法的类。默认 `BYPASSING` 选择可能绕过父实现的覆盖者。构造器注入会检查全部子类，调用接口默认实现的实现类也可获得动态注入。若某个方法仅在部分路径调用 `super`，这些路径可能先触发父类注入，再触发覆盖者上的注入。

`packages` 限定候选目标的包名前缀，`provider` 引用已注册的 `DynamicTargetProvider`。副本会解析继承的 `@Shadow` 成员，纯访问器接口由子类继承。Core 从 Loader 发现的 Mod 根路径建立类索引，扫描范围包含 Loader 已发现的嵌套 Mod。`-Dmixinsplice.targets.debug=true` 输出候选和生成数量。

`relaxRequirements = true` 将副本中的 Mixin 与 MixinExtras 注入器的 `require`、`expect` 和目标描述符的最小匹配数设为零，同时移除副本的 `@Group` 校验。原 Mixin 保留原有约束。Mixin 的 `@Group` 最小计数至少为一，因此移除分组时也会移除该组的最大计数检查。

目标分析在声明式注入调整之前执行，因此生成目标的范围取决于原始选择器和策略。

## 程序化扩展

通过 `META-INF/services/com.modularmc.mixinsplice.api.extension.SpliceExtension` 注册提供者。`ExtensionRegistry` 使用 `before` 和 `after` 排序，排序规则对已发现的扩展 ID 生效，重复 ID 或循环依赖会在初始化时报错。

`MixinCanceller` 按目标判断是否取消 Mixin。`AnnotationTransformation` 提供类或方法的只读注解视图，可通过返回新的列表添加、修改、替换和删除注解。`InjectionAnnotation` 提供对选择器、计数、注入点和选择范围的类型化编辑。自定义条件和目标提供者也可通过注册 ID 从声明式注解引用。

## 验证范围

JVM 测试验证条件求值、扩展排序和副本字节码。三平台运行用例覆盖可选目标、跨 Mixin 注入、注解作用域、动态目标、接口及插件追加的 Mixin。NeoForge 还验证真实 MixinExtras 注入器，可执行 `:loader:neoforge:runGameTestServer` 运行。

Fabric 的 `:loader:fabric:runGameTestServer` 使用 Loom 的生产服务端任务加载发行包和测试模组，在 `--initSettings` 阶段执行断言。Forge 的 `:loader:forge:runTestGameTestServer` 使用 ForgeGradle 的 `gameTestServer` 配置加载 JarJar 发行包与测试模组，由测试模组的 `@GameTest` 调用共享断言。Forge 和 NeoForge 从测试夹具读取同一个 `mixinsplice:empty` 结构模板。

PR 的 CI 依次启动 Fabric 服务端、Forge 服务端和 NeoForge GameTest 服务端，最后执行 `:tests:test`。四项结果共同决定 PR 的测试标签和作业状态。

## 发布与维护分支

`main` 跟随 MixinSplice 的最新主版本。不兼容升级时，从升级前的提交切出 `N.x` 维护分支，按需回移修复。`N` 取 MixinSplice 的主版本号。创建正式 Release 时，从对应的 `main` 或 `N.x` 创建完整版本号标签。版本更新 PR 指向 Release 记录的目标分支。

推送源码或 Gradle 配置到 `main` 和 `N.x` 时，CI 构建该次推送的提交。GitHub 快照标签使用 `latest-分支名`，例如 `latest-main` 和 `latest-1.x`。快照变更日志使用本次推送的提交范围。

CurseForge 发布需要 GitHub Actions 变量 `CURSEFORGE_ID` 和密钥 `CURSEFORGE_TOKEN`。各加载器的 Minecraft 版本清单随相应发行线的发布配置维护。

## 上游参考

MixinSplice 按现有模块和公开契约重新实现以下能力。

- MixinSquared 的 Handler 定位、取消、注解调整和排序。MIT，快照 `f9cf62f`。[参考提交](https://github.com/Bawnorton/MixinSquared/tree/f9cf62fc3f41c18ff28a72df1cbdd2a78ba4809c)
- conditional-mixin 的条件选择和版本谓词。LGPL-3.0，快照 `be560b0`。[参考提交](https://github.com/Fallen-Breath/conditional-mixin/tree/be560b03ebe94295ad496e75928e25ac9d15f8f0)
- MixinScope（PolyMixin）的动态目标、继承分析与 `super` 控制流。MIT，快照 `81af058`。[参考提交](https://github.com/Dust-Pile/polymixin/tree/81af05835f55c548581de30eb8400951f39066ec)
