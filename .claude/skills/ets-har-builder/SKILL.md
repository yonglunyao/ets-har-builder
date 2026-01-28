name: ets-har-builder
description: ETS HAR Dependency Builder - 解决 HarmonyOS HAR 模块编译依赖问题的通用 DTS 生成引擎
version: "1.0.0"

instructions: |
  你是 ETS HAR Dependency Builder 的专家助手。这个工具用于解决 HarmonyOS HAR 模块编译依赖问题，通过自动生成 stub 声明文件使模块能够通过 hvigor 编译。

  ## 核心特性

  - **通用引擎**：不针对任何特定第三方库硬编码类型，适用于任意依赖
  - **静态分析**：通过解析代码识别所有外部依赖引用
  - **类型推断**：基于通用规则推断类型，而非特定库的固定映射
  - **接口定义**：为所有引用的接口类型自动生成完整定义
  - **迭代演进**：支持增量式语法支持扩展，遇到新场景可无缝添加

  ## 开发模式（强制要求）

  ### 1. 必须先进入 plan 模式

  在开始任何开发任务之前，**必须**先使用 EnterPlanMode 工具进入 **plan 模式**进行整体方案规划。

  规划阶段必须完成以下工作：

  1. **充分理解 TypeScript DTS 语法**
     - 研究当前代码已支持的语法特性
     - 识别需要支持但尚未实现的新语法场景
     - 理解每种语法在 DTS 声明文件中的作用和格式

  2. **语法场景分析**
     - 列出所有需要支持的 DTS 语法场景
     - 对每种语法场景设计独立且可扩展的解决方案
     - 分析各场景之间的依赖关系和优先级

  3. **架构设计**
     - 设计可扩展的架构，支持未来新增语法场景
     - 定义清晰的接口和扩展点
     - 确保新增语法支持不影响现有功能

  4. **迭代计划**
     - 将任务拆分为多个可验证的迭代
     - 每个迭代专注于一种或几种语法场景
     - 明确每个迭代的验收标准

  ### 2. 迭代开发模式

  由于 DTS 文件支持的 TypeScript 语法非常复杂（包括但不限于：interface、type alias、class、enum、namespace、module、generic、conditional type、mapped type、template literal type、declaration merging 等），必须采用**迭代开发**方式：

  **迭代开发原则：**

  1. **场景驱动**：对每种或几种语法场景设计独立解决方案
     - 例如：迭代 1 处理 interface 基础语法
     - 迭代 2 处理 generic 类型参数
     - 迭代 3 处理 declaration merging
     - 迭代 4 处理 mapped types 等

  2. **增量演进**：当遇到暂不支持的场景时
     - **不要推倒原有设计重来**
     - 保持现有架构稳定
     - 通过扩展点添加新的语法支持
     - 确保向后兼容性

  3. **迭代验证循环**：每次迭代必须执行
     ```
     开发 → 单元测试 → 集成测试 → 发现问题 → 修复 → 进入下一次迭代
     ```

  4. **失败场景处理**：在集成测试时发现因方案考虑不全导致的编译报错
     - 分析报错原因，定位缺失的语法支持
     - 设计最小化的解决方案
     - 作为新的迭代任务添加
     - 完成后更新文档

  ### 3. 迭代开发示例

  #### 迭代 1：函数重载支持

  **场景**：crypto-js 库的 `encrypt` 函数有多种调用方式

  **问题**：
  ```typescript
  // 源代码中的多种调用
  CryptoJS.AES.encrypt(message, key)              // 无 options
  CryptoJS.AES.encrypt(message, key, options)     // 带 options
  ```

  **方案**：
  1. 在 `InterfaceMethodInfo` 中添加 `overloads` 列表
  2. 在 `ArkTSParser` 中识别同一函数的多次调用模式
  3. 在 `DtsGenerator` 中生成重载声明

  **验证**：
  ```typescript
  // 生成的 DTS
  namespace AES {
      function encrypt(message: string, key: string): UnknownInterface;
      function encrypt(message: string, key: string, options: UnknownInterface): UnknownInterface;
  }
  ```

  #### 迭代 2：泛型基础支持

  **场景**：现代库大量使用泛型，如 `Observable<T>`

  **问题**：
  ```typescript
  // 源代码
  interface Observable<T> {
      subscribe(callback: (value: T) => void): void;
  }
  const obs: Observable<string>;
  ```

  **方案**：
  1. 在 `TypeInfo` 中添加 `typeParameters` 字段
  2. 在 `ArkTSParser` 中识别 `<T>` 语法
  3. 在 `DtsGenerator` 中生成泛型声明

  **验证**：
  ```typescript
  // 生成的 DTS
  interface Observable<T> {
      subscribe(callback: (value: T) => void): void;
  }
  ```

  #### 迭代 3：类型别名（type）支持

  **场景**：库中使用 type 定义复杂类型

  **问题**：
  ```typescript
  // 源代码
  type Config = {
      readonly: boolean;
      deep?: boolean;
  };
  ```

  **方案**：
  1. 添加 `TypeAliasInfo` 模型
  2. 在 `ArkTSParser` 中识别 `type` 关键字
  3. 在 `DtsGenerator` 中生成 type 声明

  **验证**：
  ```typescript
  // 生成的 DTS
  type Config = {
      readonly: boolean;
      deep?: boolean;
  };
  ```

  #### 迭代 4：枚举支持

  **场景**：配置型常量使用 enum

  **问题**：
  ```typescript
  // 源代码
  enum Encoding {
      Utf8 = 'utf8',
      Base64 = 'base64',
      Hex = 'hex'
  }
  ```

  **方案**：
  1. 添加 `EnumInfo` 模型
  2. 在 `ArkTSParser` 中识别 `enum` 关键字
  3. 在 `DtsGenerator` 中生成 enum 声明

  **验证**：
  ```typescript
  // 生成的 DTS
  enum Encoding {
      Utf8 = 'utf8',
      Base64 = 'base64',
      Hex = 'hex'
  }
  ```

  #### 迭代 5：声明合并

  **场景**：通过多次定义扩展接口

  **问题**：
  ```typescript
  // 源代码
  interface Window {
      title: string;
  }
  interface Window {
      width: number;
  }
  ```

  **方案**：
  1. 在 `DependencyScanner` 中合并同名接口的属性
  2. 在 `DtsGenerator` 中生成合并后的接口

  **验证**：
  ```typescript
  // 生成的 DTS
  interface Window {
      title: string;
      width: number;
  }
  ```

  ### 4. 切换到 code 模式

  规划完成并获得用户批准后，切换到 **code 模式**开始编码：

  1. 按照迭代计划逐步实现
  2. 每完成一个迭代立即进行验证
  3. 发现问题及时修复
  4. 验证通过后进入下一次迭代

  ## TypeScript DTS 语法支持清单

  ### 完整语法清单

  TypeScript 声明文件（.d.ts）支持以下主要语法，理解这些是开发的基础：

  | 语法类别 | 语法特性 | 描述 | 当前支持 |
  |---------|---------|------|---------|
  | **基础类型** | 基本类型 | string, number, boolean, void, null, undefined, unknown, never, any | ✅ |
  | **基础类型** | 数组类型 | T[], Array<T>, ReadonlyArray<T> | ✅ |
  | **基础类型** | 元组类型 | [T1, T2, ...] | ❌ |
  | **基础类型** | 联合类型 | T1 \| T2 \| T3 | ⚠️ 部分 |
  | **基础类型** | 交叉类型 | T1 & T2 & T3 | ❌ |
  | **基础类型** | 字面量类型 | "abc", 123, true | ❌ |
  | **接口** | 基础接口 | interface Name { ... } | ✅ |
  | **接口** | 可选属性 | property?: type | ✅ |
  | **接口** | 只读属性 | readonly property: type | ⚠️ 部分 |
  | **接口** | 索引签名 | [key: string]: type | ✅ |
  | **接口** | 方法签名 | method(args): return | ✅ |
  | **接口** | 调用签名 | (args): return | ❌ |
  | **接口** | 构造签名 | new (args): return | ❌ |
  | **接口** | 泛型接口 | interface Name<T> { ... } | ❌ |
  | **接口** | 继承 | interface B extends A { ... } | ❌ |
  | **类型别名** | 基础别名 | type Name = type | ❌ |
  | **类型别名** | 泛型别名 | type Name<T> = ... | ❌ |
  | **类型别名** | 条件类型 | T extends U ? X : Y | ❌ |
  | **类型别名** | 映射类型 | { [K in keyof T]: ... } | ❌ |
  | **类型别名** | 模板字面量类型 | `${Prefix}${Suffix}` | ❌ |
  | **类** | 类声明 | class Name { ... } | ❌ |
  | **类** | 泛型类 | class Name<T> { ... } | ❌ |
  | **类** | 类继承 | class B extends A { ... } | ❌ |
  | **类** | 实现接口 | class A implements B { ... } | ❌ |
  | **类** | 静态成员 | static method() {} | ⚠️ 部分 |
  | **类** | 抽象类 | abstract class ... | ❌ |
  | **枚举** | 数字枚举 | enum Name { A, B } | ❌ |
  | **枚举** | 字符串枚举 | enum Name { A = "a", B = "b" } | ❌ |
  | **枚举** | 常量枚举 | const enum ... | ❌ |
  | **命名空间** | 基础命名空间 | namespace Name { ... } | ✅ |
  | **命名空间** | 嵌套命名空间 | namespace A { namespace B { ... } } | ✅ |
  | **命名空间** | 命名空间导出 | export const x | ✅ |
  | **命名空间** | 命名空间导入 | import ... = ... | ❌ |
  | **模块** | ES模块声明 | export { ... }, export default | ✅ |
  | **模块** | 模块导出接口 | export interface ... | ✅ |
  | **模块** | 模块导出类型 | export type ... | ❌ |
  | **模块** | 导入别名 | import { a as b } from ... | ⚠️ 部分 |
  | **函数** | 函数声明 | function name(args): return | ✅ |
  | **函数** | 函数重载 | function name(args1): return1; function name(args2): return2; | ❌ |
  | **函数** | 泛型函数 | function name<T>(arg: T): T | ❌ |
  | **函数** | 可选参数 | (arg?: type) | ⚠️ 部分 |
  | **函数** | 默认参数 | (arg: type = default) | ❌ |
  | **函数** | 剩余参数 | (...args: type[]) | ❌ |
  | **泛型** | 泛型类型参数 | <T, U> | ❌ |
  | **泛型** | 泛型约束 | <T extends U> | ❌ |
  | **泛型** | 泛型默认值 | <T = string> | ❌ |
  | **泛型** | 条件类型 | T extends U ? X : Y | ❌ |
  | **泛型** | 映射类型 | { [K in keyof T]: ... } | ❌ |
  | **高级类型** | typeof 操作符 | typeof variable | ❌ |
  | **高级类型** | keyof 操作符 | keyof T | ❌ |
  | **高级类型** | infer 类型 | infer T | ❌ |
  | **高级类型** | Pick<T, K> | ⚠️ 部分 |
  | **高级类型** | Partial<T> | ⚠️ 部分 |
  | **高级类型** | Required<T> | ⚠️ 部分 |
  | **高级类型** | Readonly<T> | ⚠️ 部分 |
  | **声明合并** | 接口合并 | 多个 interface 相同名称 | ❌ |
  | **声明合并** | 命名空间与函数/类合并 | ❌ | ❌ |
  | **全局声明** | declare global | 全局类型声明 | ✅ |
  | **全局声明** | declare module | 模块声明 | ⚠️ 部分 |
  | **其他** | import type | import type { ... } from ... | ❌ |
  | **其他** | export as namespace | export as namespace NS | ❌ |
  | **其他** | 类型断言 | value as type | ⚠️ 部分 |
  | **其他** | 非空断言 | value! | ❌ |
  | **其他** | 装饰器 | @decorator | ❌ |

  ### 当前已支持语法

  1. **基础类型**：string, number, boolean, void, unknown
  2. **数组类型**：T[] 基础形式
  3. **接口**：
     - 基础接口定义
     - 可选属性 `property?: type`
     - 索引签名 `[key: string]: type`
     - 方法签名 `method(args): return`
  4. **命名空间**：
     - 基础命名空间声明
     - 嵌套命名空间
     - 导出成员
  5. **函数**：
     - 基础函数声明
     - 简单类型参数
  6. **模块**：
     - ES模块导出 `export { ... }`
     - 默认导出 `export default`
     - 导出接口 `export interface`
  7. **全局声明**：`declare global` 格式

  ### 待支持的高优先级语法

  1. **泛型支持**：泛型接口、泛型函数、泛型约束
  2. **函数重载**：同一函数多种签名
  3. **类型别名**：type 声明、条件类型
  4. **类声明**：class 语法、构造签名
  5. **枚举**：enum 声明和值
  6. **声明合并**：接口合并能力
  7. **映射类型**：Pick, Partial, Omit 等工具类型
  8. **更复杂的数组/对象类型**：元组、联合类型、交叉类型

  ### 语法实现优先级建议

  基于实际使用场景和复杂度，建议的优先级：

  | 优先级 | 语法特性 | 原因 |
  |-------|---------|------|
  | P0 | 函数重载 | 常见库如 crypto-js 有大量重载 |
  | P0 | 泛型基础 | 现代库大量使用泛型 |
  | P1 | 类型别名 | type 声明很常见 |
  | P1 | 枚举 | 配置型常量常用 |
  | P1 | 只读属性 | interface 中 readonly 关键字 |
  | P2 | 类声明 | 面向对象库需要 |
  | P2 | 条件类型 | 高级类型推断需要 |
  | P2 | 映射类型 | 工具类型需要 |
  | P3 | 模板字面量类型 | 特殊场景需要 |
  | P3 | 装饰器 | 装饰器模式需要 |

  ## 核心场景

  ### 问题背景

  在以下场景中，HAR 模块无法正常编译：

  1. **封闭环境**：无法访问外部 npm 仓库或第三方组件源码
  2. **依赖缺失**：模块通过 `oh-package.json5` 声明了依赖，但无法获取实际代码
  3. **编译失败**：hvigor 编译时找不到依赖的类型声明，报错退出

  ### 解决方案

  通过静态分析模块代码，识别出对外部依赖的所有引用，然后为每个依赖生成：

  ```
  <dependency-path>/
  ├── Index.d.ts           # TypeScript 类型声明文件
  ├── oh-package.json5     # HarmonyOS 包配置
  └── build-profile.json5  # 构建配置
  ```

  生成的 `Index.d.ts` 包含：
  - 引用的函数签名（参数和返回值类型）
  - 成员变量字段类型
  - 嵌套 namespace 结构
  - 严格类型约束（禁止 `any` 作为返回值）

  ## 关键约束

  ### ArkTS 强类型校验

  ArkTS 是强类型语言，对依赖模块有以下要求：

  | 位置 | 允许类型 | 说明 |
  |------|----------|------|
  | 入参 | `any` | 允许使用 any 类型作为参数 |
  | 返回值 | **明确类型** | **禁止使用 any，必须是具体类型** |
  | 接口类型 | **必须定义** | **引用的接口必须有完整定义** |

  **错误示例**：
  ```typescript
  // ❌ 编译失败：返回值是 any
  function encrypt(data: string): any;
  ```

  **正确示例**：
  ```typescript
  // ✅ 编译通过：接口有定义，返回值是明确类型
  interface UnknownInterface {
      [key: string]: unknown;
  }
  function encrypt(data: string): UnknownInterface;
  ```

  ### 接口类型定义规则

  **核心规则：所有引用的接口类型必须有完整定义**

  当函数签名或属性类型引用了接口类型时，该接口必须在 `Index.d.ts` 中完整定义。引擎会自动：

  1. **收集所有引用的接口类型**：扫描返回值类型和函数签名
  2. **生成基础接口定义**：为每个引用的接口生成基本定义
  3. **添加静态方法**：从静态方法调用中识别并添加静态方法

  **生成的接口定义格式**：
  ```typescript
  // 1. 首先定义所有接口类型（在 declare global 之前）
  interface WordArray {
      words: number[];
      sigBytes: number;
      toString(encoder?: unknown): string;
      clone(): WordArray;

      // 从静态方法调用中识别的静态方法
      static random(nBytes: number): WordArray;
  }

  interface UnknownInterface {
      [key: string]: unknown;
  }

  // 2. 然后是 namespace 声明
  declare global {
      namespace RootNamespace {
          export const WordArray: WordArray;
      }
  }
  ```

  ### 静态方法识别

  引擎能够自动识别静态方法调用并为接口添加静态方法定义：

  **识别模式**：
  ```
  完整路径: Namespace.ClassName.staticMethod(args)
     ↓
  接口名: ClassName
  方法名: staticMethod
  是否静态: true
  参数: 根据方法名推断
  返回值: 根据方法名推断
  ```

  **示例**：
  ```typescript
  // 源代码
  const randomData = RootNamespace.lib.WordArray.random(16);

  // 自动生成的接口定义
  interface WordArray {
      static random(nBytes: number): WordArray;
  }
  ```

  ## 工作流程

  ```
  ┌─────────────────┐
  │  HAR 模块源码   │
  │  (ArkTS/TS)     │
  └────────┬────────┘
           │
           ▼
  ┌─────────────────┐
  │  依赖扫描器      │
  │  Scanner         │
  └────────┬────────┘
           │
           ▼
  ┌─────────────────┐
  │  类型推断器      │
  │  TypeInferrer    │
  └────────┬────────┘
           │
           ▼
  ┌─────────────────┐
  │  DTS 生成器      │
  │  Generator       │
  └────────┬────────┘
           │
           ▼
  ┌─────────────────┐
  │  Index.d.ts      │
  │  oh-package.json5 │
  └─────────────────┘
           │
           ▼
  ┌─────────────────┐
  │  hvigor 编译     │
  └─────────────────┘
           │
           ▼
       ✓ 编译成功
  ```

  ## 复杂类型支持

  ### 嵌套 Namespace

  支持解析和生成多层嵌套的 namespace 结构：

  ```typescript
  // 源代码中的使用
  import { RootNamespace } from '@ohos/example';
  let result = RootNamespace.enc.Utf8.parse(data);
  ```

  ```typescript
  // 生成的 Index.d.ts
  declare global {
      namespace RootNamespace {
          export namespace enc {
              const Utf8: Encoder;
          }
      }
  }

  interface Encoder {
      parse(str: string): UnknownInterface;
      stringify(wordArray: UnknownInterface): string;
  }
  ```

  ### 类型推断规则

  | 使用模式 | 推断类型 | 返回值类型 |
  |----------|----------|----------|
  | `ClassName.method()` | function | unknown |
  | `ClassName.staticMethod()` | function | 接口名 |
  | `const ConstantName` | const | 接口名或unknown |

  ## 使用方法

  ### 命令行使用

  ```bash
  # 编译项目
  mvn clean package -DskipTests

  # 处理模块（生成依赖 stub）
  java -jar target/ets-har-builder-1.0-SNAPSHOT.jar <模块路径>

  # 示例
  java -jar target/ets-har-builder-1.0-SNAPSHOT.jar "D:\\code\\secueity\\abc_har\\myutils"
  ```

  ### Java API 使用

  ```java
  import org.example.harmony.HarDependencyEngine;

  // 创建引擎实例
  HarDependencyEngine engine = new HarDependencyEngine();

  // 处理模块
  HarDependencyEngine.EngineResult result = engine.process("D:\\code\\secueity\\abc_har\\myutils");

  // 检查结果
  if (result.isSuccess()) {
      System.out.println("生成依赖数量: " + result.getDependencies().size());
      System.out.println("输出路径: " + result.getProjectRootPath());
  }
  ```

  ### 编译生成的 HAR

  ```bash
  # 进入模块目录
  cd "D:\\code\\secueity\\abc_har"

  # 使用 hvigor 编译
  node "C:\\Program Files\\Huawei\\DevEco Studio\\tools\\hvigor\\bin\\hvigorw.js" \
    --mode module -p product=default -p module=myutils@default \
    assembleHar --analyze=normal --parallel --incremental --daemon
  ```

  ## 项目结构

  ```
  ets-har-builder/
  ├── pom.xml                                  # Maven 配置
  ├── src/main/java/org/example/harmony/
  │   ├── HarDependencyEngine.java             # 主引擎入口
  │   ├── model/                               # 数据模型
  │   │   ├── TypeInfo.java                   # 类型信息（支持 parentNamespace）
  │   │   ├── MemberAccessInfo.java           # 成员访问信息
  │   │   ├── ImportInfo.java                 # Import 信息
  │   │   ├── DependencyInfo.java             # 依赖信息聚合
  │   │   ├── ArkTSParseResult.java           # 解析结果容器
  │   │   └── InterfaceMethodInfo.java         # 接口方法信息
  │   ├── parser/
  │   │   └── ArkTSParser.java                # ArkTS/TS 解析器
  │   ├── scanner/
  │   │   └── DependencyScanner.java          # 依赖扫描器
  │   └── generator/
  │       ├── DtsGenerator.java               # DTS 文件生成器
  │       └── DependencyStructureGenerator.java # 目录结构生成器
  ├── src/main/resources/templates/
  │   └── build-profile.json5                 # 构建配置模板
  ├── src/test/java/                          # 测试代码
  ├── src/test/resources/                     # 测试资源
  └── .claude/skills/                         # 本技能文档
  ```

  ## 核心类说明

  ### HarDependencyEngine

  主引擎类，负责协调整个处理流程。

  ```java
  // 处理模块，生成依赖 stub
  EngineResult process(String modulePath) throws IOException

  // 返回结果包含：
  // - modulePath: 处理的模块路径
  // - projectRootPath: 项目根目录（依赖生成的位置）
  // - dependencies: 生成的依赖列表
  // - success: 是否成功
  ```

  ### ArkTSParser

  解析 ArkTS/TypeScript 源文件。

  ```java
  // 解析单个文件
  ArkTSParseResult parse(Path filePath) throws IOException

  // 提取的信息：
  // - import 语句（命名导入、默认导入、命名空间导入）
  // - export 语句
  // - 成员访问表达式（支持嵌套如 CryptoJS.enc.Utf8.parse）
  ```

  **正则表达式模式**：
  - `IMPORT_PATTERN`: 匹配 import 语句
  - `MEMBER_ACCESS_PATTERN`: 匹配成员访问（支持多层嵌套）
  - `METHOD_CALL_PATTERN`: 匹配方法调用
  - `PROPERTY_ACCESS_PATTERN`: 匹配属性访问
  - `STATIC_METHOD_PATTERN`: 匹配静态方法调用（如 WordArray.random）

  ### DependencyScanner

  扫描模块并收集依赖信息。

  ```java
  // 扫描模块目录
  Map<String, DependencyInfo> scan(String modulePath) throws IOException

  // 关键方法：
  // - buildNamespaceStructure(): 构建嵌套namespace结构
  // - processStaticMethodCall(): 处理静态方法调用
  // - inferMemberKind(): 推断成员类型
  // - inferMethodReturnType(): 推断方法返回值类型
  ```

  ### DtsGenerator

  生成 TypeScript 声明文件。

  ```java
  // 生成 Index.d.ts
  void generate(DependencyInfo dependency, String outputPath) throws IOException

  // 生成 oh-package.json5
  void generateOhPackageJson(DependencyInfo dependency, String outputPath) throws IOException

  // 关键方法：
  // - generateGlobalNamespaceDeclaration(): 生成 declare global 格式
  // - generateRegularDeclaration(): 生成普通格式
  // - generateTypeInterfaces(): 生成接口类型定义
  ```

  ## 生成文件示例

  ### Index.d.ts 格式（通用）

  ```typescript
  // Auto-generated by ets-har-builder

  // 1. 首先定义所有接口类型
  interface UnknownInterface {
      [key: string]: unknown;
  }

  interface WordArray {
      words: number[];
      sigBytes: number;
      toString(encoder?: UnknownInterface): string;
      clone(): WordArray;

      static random(nBytes: number): WordArray;
  }

  // 2. 然后是 namespace 声明
  declare global {
      namespace RootNamespace {
          export namespace enc {
              const Utf8: UnknownInterface;
          }
      }
  }

  export default RootNamespace;
  export { RootNamespace };
  ```

  ## 限制和注意事项

  1. **正则表达式解析**：使用正则表达式解析代码，可能不支持所有语法变体
  2. **类型推断**：基于启发式规则，生成的类型签名可能需要手动调整
  3. **仅生成 stub**：生成的 DTS 文件仅包含声明，不包含实现
  4. **相对路径依赖**：不处理相对路径的 import（如 `./utils`）
  5. **复杂泛型**：对复杂泛型类型的支持有限
  6. **参考文件可用**：对于已知库，可以在 `KNOWN_LIBRARY_TEMPLATES` 中预定义模板

  ## 后续改进方向

  1. 支持 TypeScript AST 解析，提高解析准确性
  2. 支持更多类型推断模式
  3. 支持自定义 DTS 模板
  4. 支持增量更新（只更新变更的依赖）
  5. 支持配置文件自定义行为
  6. 改进复杂泛型类型的处理

references:
  - title: HarmonyOS 官方文档
    url: https://developer.huawei.com/consumer/cn/doc/harmonyos-guides-V5/
  - title: TypeScript 声明文件文档
    url: https://www.typescriptlang.org/docs/handbook/declaration-files/do-s-and-don-ts.html
