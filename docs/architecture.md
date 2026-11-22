# MixinWeaver 架构

MixinWeaver 是 SpongePowered Mixin 的扩展。动态目标发现、跨 Mixin 注入和条件选择共用 MixinWeaver 的注册、排序、生命周期、运行上下文和诊断设施。PolyMixin、MixinSquared 和 ConditionalMixin 提供能力参考，具体契约由 MixinWeaver 定义。

当前阶段完成项目命名、Gradle 模块划分和 NeoForge 模板迁移。以下生命周期、子系统及接口约束用于后续源码审查和实现。

## 模块边界

| Gradle 模块 | 职责 | 当前状态 |
| --- | --- | --- |
| `:api` | 公开扩展契约和稳定的数据模型 | Java 库模块 |
| `:core` | 注册、排序、生命周期、诊断、子系统规则和 Mixin 对接 | Java 库模块，依赖 API，编译期使用 Mixin |
| `:loader:fabric`、`:loader:forge` | 对应平台的启动接入、开发运行和发行 | Gradle 模块结构 |
| `:loader:neoforge` | NeoForge 入口、模组元数据、开发运行、GameTest 和发行 | 使用 ModDevGradle，生成主 JAR、源码 JAR 和独立的 slim JAR |
| `:tests` | 单元、字节码、Mixin 生命周期和集成测试 | JVM 测试项目，NeoForge GameTest 由 Loader 模块运行 |

依赖方向为 `Loader → Core → API`。公开 API 保持 Loader 独立，ASM 与 Mixin 的内部对象由 Core 管理。`com.modularmclib.mixinweaver.core.mixins` 是 Mixin 配置加载注入类的包。版本目录锁定 Mixin `0.8.7`，NeoForge 开发环境使用暴露相同 API 版本的发行包。未来更新先验证所用的 Mixin 接口和执行时机，再更新锁定版本。历史兼容层和 MixinExtras 在此设计范围之外。

项目显示名为 **MixinWeaver**，Mod ID 为 `mixinweaver`，Java 命名空间为 `com.modularmclib.mixinweaver`。当前 NeoForge 模板目标为 Minecraft `1.21.1` 和 Java 21。NeoForge 平台模块将自身产物与 API、Core 一同打包，Minecraft、Mixin 和平台依赖由运行环境提供。公共打包脚本用平台子项目名作为 Loader 标识，按 `MixinWeaver-<loader>--<minecraft>-<version>.jar` 命名。

## Mixin 生命周期

| 阶段 | 接入位置 | 职责 |
| --- | --- | --- |
| 初始化 | Loader 启动入口和 Core | 创建共享注册表，验证所需 Mixin 能力，注册转换扩展 |
| 准备和选择 | `IMixinConfigPlugin` 及受验证的 Mixin 准备阶段接口 | 条件求值、取消 Mixin、解析和应用动态目标 |
| 注入和转换 | `IExtension` 及受验证的注入准备阶段接口 | Handler 定位、注解变换、注入策略和字节码转换 |
| 导出 | `IExtension.export()` | 输出转换诊断或导出结果 |

`IMixinConfigPlugin.shouldApplyMixin()` 在选择阶段决定某个 Mixin 是否应用。`getMixins()` 和 `acceptTargets()` 提供配置和目标相关回调，动态目标扩张依赖的实际接入点待源码审查确认。`IExtension.checkActive()` 决定扩展启用状态，`preApply()` 和 `postApply()` 划定转换阶段。条件求值须在目标字节码进入转换前完成，Handler 和注解处理须确认其所需状态在对应时点可见。

两个入口共享扩展注册表和执行上下文。Mixin 的 `IExtensionRegistry` 提供查询，扩展添加由具体的 `Extensions` 提供。Core 需验证注册时机、活动扩展列表与排序的对应关系，以及当前 Mixin 运行时是否满足所需能力。

MixinWeaver 根据扩展声明的 `before` 和 `after` 约束确定执行顺序，并在初始化时诊断冲突。Core 将结果映射到 Mixin 可控的阶段和扩展顺序，并报告运行时无法满足的顺序约束。其他 Mod 的扩展顺序由 Mixin 管理。

## 子系统

| 子系统 | 能力来源 | 职责 |
| --- | --- | --- |
| Target | PolyMixin | 发现目标、分析继承与控制流、生成目标扩展结果 |
| Handler | MixinSquared | 定位 Mixin Handler，表达跨 Mixin 引用 |
| Condition | ConditionalMixin | 解析条件表达式并决定 Mixin 应用资格 |
| Injection | PolyMixin、MixinSquared | 处理跨 Handler 注入和动态目标的注入约束 |
| Transformation | MixinSquared | 以受控操作修改注解、方法及类节点 |
| Extension | 三套实现的公共需求 | 扩展注册、执行排序、生命周期和诊断 |

### 目标扩展

目标处理依次完成类发现、继承关系分析、方法控制流分析和候选选择，最后由 Core 将目标集合应用到 Mixin。目标基类方法的继承者与始终调用父类实现的覆盖者通常已受到基类注入影响，绕过父类实现的覆盖者可能需要单独注入。部分路径调用父类、部分路径绕过时，需要验证重复触发的边界。

目标选择需支持子类、接口实现、方法覆盖和 `super` 调用，考虑 Refmap、运行时可见类路径、嵌套 JAR 和无法准确推导的选择器。注入策略负责生成目标所需的 `require` 调整。扫描结果通过 MixinWeaver 的目标模型传递，以保持公共 API 独立于 ClassGraph。

### Handler 与转换

Handler 选择器可组合 Mixin 配置、Mixin 类名、方法名和描述符，定位结果携带稳定的 Handler 引用。审查时需验证同名与重载方法、多配置目标、类名映射及跨 Mixin 注入时的查找顺序。

注解转换使用只读注解视图及明确的增加、修改、替换和删除操作。过滤条件可使用 Mixin、目标类、方法及注解描述符，诊断记录需说明操作和作用对象。共享 ASM 节点由转换阶段管理。

### 条件选择

条件可表达 Mod 存在、Mod 版本、Mixin 存在、自定义判断及布尔组合。求值结果包含 `TRUE`、`FALSE` 和 `UNKNOWN`，其中 `UNKNOWN` 表示信息尚未初始化或当前环境无法确定。

任一必需条件为 `FALSE` 或冲突条件为 `TRUE` 时拒绝应用。其余情况下按 `UnknownPolicy` 处理 `UNKNOWN`，默认使用 `DENY`，可显式选择 `ALLOW` 或 `FAIL`。必需条件全部满足、冲突条件全部排除时允许应用，空条件同样允许应用。

## 源码审查清单

在冻结公共 API 前，先审查 PolyMixin 的 `polymixin-core`，再核对 MixinSquared 和 ConditionalMixin 的实际实现。

### PolyMixin

- [ ] `DynamicTargets` 注解、默认 Provider 和自定义目标上下文
- [ ] ClassGraph 扫描、运行时可见范围和嵌套 JAR 发现
- [ ] Refmap 方法名解析和注入要求调整
- [ ] 继承关系和接口实现分析
- [ ] `super` 调用与控制流分析

### MixinSquared

- [ ] `TargetHandler` 定位规则
- [ ] Annotation Adjuster 的执行时机
- [ ] Mixin Canceller 的实现和应用时机
- [ ] Extension 注册和排序机制

### ConditionalMixin

- [ ] `Restriction` 和 `Condition` 的完整语义
- [ ] Mod ID、版本谓词和缓存
- [ ] Mixin 存在性条件
- [ ] 自定义 Condition Tester
- [ ] Restriction 注解清理

### 统一集成

- [ ] `IExtension` 与 Config Plugin 的双入口协调
- [ ] 扩展依赖排序和冲突处理
- [ ] Loader 启动和平台发行机制

## 参考源码

- 动态目标发现和方法分析。 [PolyMixin](https://github.com/Dust-Pile/polymixin)
- Handler 选择、注解调整和扩展排序。 [MixinSquared](https://github.com/Bawnorton/MixinSquared)
- 条件匹配和版本谓词。 [ConditionalMixin](https://github.com/Fallen-Breath/conditional-mixin)
- Mixin 生命周期接口。 [IExtension](https://github.com/SpongePowered/Mixin/blob/master/src/main/java/org/spongepowered/asm/mixin/transformer/ext/IExtension.java) · [IMixinConfigPlugin](https://github.com/SpongePowered/Mixin/blob/master/src/main/java/org/spongepowered/asm/mixin/extensibility/IMixinConfigPlugin.java)
