# 系统架构

## 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    HarDependencyEngine                       │
│                      (主引擎)                                 │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────┐          ┌──────────────────┐
│ DependencyScanner│          │  DtsGenerator     │
│   (依赖扫描)       │          │  (DTS生成)        │
└────────┬─────────┘          └────────┬─────────┘
         │                              │
         │         ┌────────────┐       │
         └────────►│ArkTSParser │───────┘
                   │ (代码解析)  │
                   └────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌──────────────────┐          ┌──────────────────┐
│  Source Files    │          │   Model Classes  │
│  .ets, .ts       │          │  TypeInfo,       │
└──────────────────┘          │  ImportInfo,     │
                              │  DependencyInfo  │
                              └──────────────────┘
```

## 数据流图

```
1. 输入: 模块路径
   │
   ▼
2. DependencyScanner 扫描源文件
   │
   ├─► 查找所有 .ets 和 .ts 文件
   │
   ▼
3. ArkTSParser 解析每个文件
   │
   ├─► 提取 import 语句 → ImportInfo
   ├─► 提取 export 语句 → TypeInfo
   └─► 提取成员访问 → MemberAccessInfo
   │
   ▼
4. 构建 DependencyInfo
   │
   ├─► 按 modulePath 分组
   ├─► 构建命名空间层级
   └─► 推断类型信息
   │
   ▼
5. DtsGenerator 生成文件
   │
   ├─► 生成 Index.d.ts (DTS 声明文件)
   ├─► 生成 oh-package.json5 (包配置)
   └─► 生成 build-profile.json5 (构建配置)
   │
   ▼
6. 输出: EngineResult
   │
   └─► 包含生成的依赖信息和状态
```

## 核心组件详解

### 1. HarDependencyEngine (引擎核心)

**职责:**
- 协调整个处理流程
- 确定项目根目录和输出路径
- 过滤 SDK 依赖
- 更新模块的 oh-package.json5

**关键流程:**
```java
process(modulePath)
    ├─► 确定项目根目录
    ├─► 扫描依赖 (DependencyScanner)
    ├─► 过滤 SDK 依赖
    ├─► 生成 DTS 文件 (DtsGenerator)
    ├─► 更新模块配置
    └─► 返回 EngineResult
```

### 2. DependencyScanner (依赖扫描器)

**职责:**
- 查找所有源文件
- 协调解析过程
- 构建 DependencyInfo 对象
- 构建命名空间层级结构

**关键方法:**
```java
scan(modulePath)
    ├─► findSourceFiles()  // 查找 .ets, .ts 文件
    ├─► parse() for each file
    └─► collectDependencies()
        ├─► 处理 import 语句
        └─► 处理成员访问表达式
```

### 3. ArkTSParser (代码解析器)

**职责:**
- 解析 ArkTS/TypeScript 源代码
- 提取 import 语句
- 提取 export 语句
- 解析成员访问表达式

**解析模式:**

**Import 解析:**
```
import { CryptoJS } from '@ohos/crypto-js'
       ↓
ImportInfo {
    modulePath: "@ohos/crypto-js",
    importedName: "CryptoJS",
    localAlias: null,
    isTypeImport: false
}
```

**成员访问解析:**
```
CryptoJS.enc.Utf8.parse(data)
       ↓
MemberAccessInfo {
    baseObject: "CryptoJS",
    fullPath: "CryptoJS.enc.Utf8.parse",
    memberName: "parse",
    context: METHOD_CALL
}
```

### 4. DtsGenerator (DTS 生成器)

**职责:**
- 生成 TypeScript 声明文件
- 构建命名空间树结构
- 生成包配置文件

**生成流程:**
```java
generate()
    ├─► 检查预定义模板
    ├─► 判断导入类型 (namespace vs named)
    ├─► buildNamespaceTree()  // 构建层级树
    │   ├─► 按父命名空间分组
    │   ├─► 识别有子成员的类型
    │   └─► 创建占位类型
    │
    └─► generateNamespaceContent()  // 递归生成
        ├─► 生成 namespace 声明
        ├─► 生成 const 声明
        └─► 生成 function 声明
```

## 数据模型

### TypeInfo (类型信息)

```java
class TypeInfo {
    String name;                // 类型名称
    String kind;                // function | const | class | interface
    String signature;           // 类型签名
    String returnType;          // 返回值类型
    String parentNamespace;     // 父命名空间路径
    List<TypeInfo> children;    // 子类型列表
}
```

**命名空间示例:**
```
parse: {
    name: "parse",
    kind: "function",
    parentNamespace: "CryptoJS.enc.Utf8",
    returnType: "WordArray"  // 明确类型，禁止使用 object/any
}

Utf8: {
    name: "Utf8",
    kind: "const",
    parentNamespace: "CryptoJS.enc",
    children: [parse, stringify]
}

enc: {
    name: "enc",
    kind: "const",
    parentNamespace: "CryptoJS",
    children: [Utf8, Hex, Base64, Latin1]
}
```

### MemberAccessInfo (成员访问信息)

```java
class MemberAccessInfo {
    String baseObject;      // 基础对象名 (如 "CryptoJS")
    String fullPath;        // 完整访问路径 (如 "CryptoJS.enc.Utf8.parse")
    String memberName;      // 最后的成员名 (如 "parse")
    AccessContext context;  // METHOD_CALL | PROPERTY_ACCESS
    String callSignature;   // 调用签名
}
```

### DependencyInfo (依赖信息)

```java
class DependencyInfo {
    String modulePath;              // 模块路径 (如 "@ohos/crypto-js")
    List<ImportInfo> importInfos;   // 所有 import 信息
    List<TypeInfo> referencedTypes; // 所有引用的类型
}
```

## 命名空间处理

### 层级结构构建

对于访问路径 `CryptoJS.enc.Utf8.parse`:

```java
// 在 DependencyScanner.buildNamespaceStructure() 中
String[] parts = {"CryptoJS", "enc", "Utf8", "parse"};

// 创建中间命名空间
for (int i = 1; i < parts.length - 1; i++) {
    // i=1: 创建 "enc" with parentNamespace="CryptoJS"
    // i=2: 创建 "Utf8" with parentNamespace="CryptoJS.enc"
}

// 创建最终成员
// 创建 "parse" with parentNamespace="CryptoJS.enc.Utf8"
```

### 树结构生成

在 DtsGenerator 中:

```java
Map<String, List<TypeInfo>> tree = {
    "CryptoJS": [enc, ...],
    "CryptoJS.enc": [Utf8, ...],
    "CryptoJS.enc.Utf8": [parse, stringify],
    ...
}
```

生成时递归遍历树:

```typescript
namespace CryptoJS {
    export namespace enc {
        const Utf8 = ...;
        function parse(...);
    }
}
```

## 类型推断规则

### 基于访问上下文

| 上下文 | 推断类型 | 示例 |
|--------|----------|------|
| METHOD_CALL | function | `obj.method()` |
| PROPERTY_ACCESS | const | `obj.property` |
| 首字母大写 | class | `import { Abc }` |
| 已知编码器名 | const with methods | `Utf8`, `Hex` |

### 基于名称模式

```java
// 函数返回值推断 - 必须返回明确类型
inferMethodReturnType(methodName) {
    if (methodName.equals("parse")) return "WordArray";
    if (methodName.equals("stringify")) return "string";
    if (methodName.equals("encrypt")) return "CipherParams";
    if (methodName.equals("decrypt")) return "WordArray";
    // 默认返回明确的类型，禁止使用 object/any
    return "WordArray";
}

// 常量类型推断 - 返回接口类型
inferConstType(constName) {
    if (constName.matches("Utf8|Hex|Base64|Latin1"))
        return constName;  // 如 "Utf8" -> 返回 Utf8 接口类型
    // ...
}
```

## 扩展点

### 添加自定义类型模板

在 `DtsGenerator.java` 中编辑 `KNOWN_LIBRARY_TEMPLATES`:

```java
private static final Map<String, String> KNOWN_LIBRARY_TEMPLATES = Map.of(
    "my-library",
    """
    export interface MyType {
        prop: string;
        method(arg: number): void;
    }
    """
);
```

### 添加 SDK 前缀过滤

在 `HarDependencyEngine.java` 中编辑 `SDK_PREFIXES`:

```java
private static final String[] SDK_PREFIXES = {
    "@kit.",
    "@ohos.",
    "@hms",
    "@mycompany/sdk"  // 添加自定义前缀
};
```

### 自定义类型推断

扩展 `DtsGenerator.java` 中的推断方法:

```java
private String inferConstType(String constName) {
    // 添加自定义规则
    if (constName.equals("MyCustomType")) {
        return "{ customMethod(): void }";
    }
    // ...
}
```

## 性能考虑

1. **文件扫描**: 使用 `Files.walk()` 并行扫描
2. **正则匹配**: 预编译所有 Pattern 常量
3. **去重检查**: 使用 Stream.anyMatch() 快速检查
4. **TreeMap**: 对命名空间排序，保证输出一致性

## 限制和已知问题

1. **正则解析**: 不支持所有 TypeScript 语法变体
2. **类型推断**: 启发式规则，可能不准确
3. **泛型支持**: 有限的泛型类型支持
4. **重载函数**: 不支持函数重载检测

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
