# API 参考手册

## HarDependencyEngine

主引擎类，负责协调整个依赖处理流程。

### 构造函数

```java
public HarDependencyEngine()
```

创建一个新的引擎实例。

### 方法

#### process

```java
public EngineResult process(String modulePath)
```

处理指定的模块路径，扫描并生成所有外部依赖的 DTS 文件。

**参数:**
- `modulePath`: HAR 模块的根目录路径

**返回值:**
- `EngineResult`: 包含处理结果的对象

**示例:**
```java
HarDependencyEngine engine = new HarDependencyEngine();
EngineResult result = engine.process("D:\\code\\myproject\\mymodule");

if (result.isSuccess()) {
    System.out.println("成功生成 " + result.getDependencies().size() + " 个依赖");
} else {
    System.err.println("处理失败");
}
```

---

## EngineResult

处理结果类，包含处理的状态和结果信息。

### 方法

#### isSuccess

```java
public boolean isSuccess()
```

返回处理是否成功。

#### getDependencies

```java
public Map<String, DependencyInfo> getDependencies()
```

返回生成的依赖映射（模块路径 -> DependencyInfo）。

#### getModulePath

```java
public String getModulePath()
```

返回被处理的模块路径。

#### getProjectRootPath

```java
public String getProjectRootPath()
```

返回项目根路径（依赖生成的位置）。

---

## DependencyInfo

依赖信息类，描述单个外部依赖。

### 方法

#### getModulePath

```java
public String getModulePath()
```

返回依赖的模块路径（如 `@ohos/crypto-js`）。

#### getImportInfos

```java
public List<ImportInfo> getImportInfos()
```

返回该依赖的所有 import 信息列表。

#### getReferencedTypes

```java
public List<TypeInfo> getReferencedTypes()
```

返回该依赖中被引用的所有类型。

---

## TypeInfo

类型信息类，描述代码中使用的一个类型。

### 方法

#### getName

```java
public String getName()
```

返回类型名称（如 `Utf8`, `MD5`）。

#### getKind

```java
public String getKind()
```

返回类型种类：`function`, `const`, `class`, `interface`, `namespace` 等。

#### getParentNamespace

```java
public String getParentNamespace()
```

返回父命名空间路径（如 `CryptoJS.enc`）。

#### getSignature

```java
public String getSignature()
```

返回类型签名（用于代码生成）。

#### getReturnType

```java
public String getReturnType()
```

返回函数的返回值类型（仅函数类型）。

---

## ImportInfo

Import 信息类，描述一个 import 语句。

### 方法

#### getModulePath

```java
public String getModulePath()
```

返回导入的模块路径。

#### getImportedName

```java
public String getImportedName()
```

返回导入的名称（命名空间导入时为 `*`）。

#### getLocalAlias

```java
public String getLocalAlias()
```

返回本地别名（如 `import * as CryptoJS` 中的 `CryptoJS`）。

#### isTypeImport

```java
public boolean isTypeImport()
```

返回是否是类型导入（`import type`）。

#### isExternalDependency

```java
public boolean isExternalDependency()
```

返回是否是外部依赖（非相对路径）。

---

## ArkTSParser

ArkTS/TypeScript 解析器。

### 方法

#### parse

```java
public ArkTSParseResult parse(Path filePath) throws IOException
```

解析指定的 ArkTS/TypeScript 文件。

**参数:**
- `filePath`: 要解析的文件路径

**返回值:**
- `ArkTSParseResult`: 包含解析结果的对象

---

## DtsGenerator

DTS 文件生成器。

### 方法

#### generate

```java
public void generate(DependencyInfo dependency, String outputPath) throws IOException
```

为单个依赖生成 DTS 文件。

**参数:**
- `dependency`: 依赖信息对象
- `outputPath`: 输出目录路径

#### generateOhPackageJson

```java
public void generateOhPackageJson(DependencyInfo dependency, String outputPath) throws IOException
```

为依赖生成 oh-package.json5 文件。

---

## DependencyScanner

依赖扫描器。

### 方法

#### scan

```java
public Map<String, DependencyInfo> scan(String modulePath) throws IOException
```

扫描模块目录，收集所有外部依赖。

**参数:**
- `modulePath`: 模块根目录路径

**返回值:**
- 依赖信息映射（模块路径 -> DependencyInfo）

---

## 配置常量

### SDK_PREFIXES

识别为 HarmonyOS SDK 的模块路径前缀：

```java
private static final String[] SDK_PREFIXES = {
    "@kit.",
    "@ohos.",
    "@hms",
    "@ohos/hvigor-ohos-plugin"
};
```

带有这些前缀的依赖会被跳过，不会生成 DTS 文件。

---

## 使用示例

### 完整流程示例

```java
import org.example.harmony.HarDependencyEngine;
import org.example.harmony.HarDependencyEngine.EngineResult;
import org.example.harmony.model.DependencyInfo;

public class Example {
    public static void main(String[] args) {
        HarDependencyEngine engine = new HarDependencyEngine();
        EngineResult result = engine.process("D:\\code\\myproject\\mymodule");

        if (result.isSuccess()) {
            System.out.println("处理成功！");
            System.out.println("模块路径: " + result.getModulePath());
            System.out.println("项目根目录: " + result.getProjectRootPath());
            System.out.println("生成依赖数: " + result.getDependencies().size());

            for (String modulePath : result.getDependencies().keySet()) {
                DependencyInfo dep = result.getDependencies().get(modulePath);
                System.out.println("  - " + modulePath + ": " +
                    dep.getReferencedTypes().size() + " 个类型");
            }
        }
    }
}
```

### 只扫描不生成文件

如果只需要分析依赖而不生成文件，可以单独使用 DependencyScanner：

```java
import org.example.harmony.scanner.DependencyScanner;
import org.example.harmony.model.DependencyInfo;
import java.nio.file.Paths;

public class ScanExample {
    public static void main(String[] args) throws IOException {
        DependencyScanner scanner = new DependencyScanner();
        var dependencies = scanner.scan("D:\\code\\myproject\\mymodule");

        dependencies.forEach((modulePath, depInfo) -> {
            System.out.println("依赖: " + modulePath);
            System.out.println("  导入数: " + depInfo.getImportInfos().size());
            System.out.println("  类型数: " + depInfo.getReferencedTypes().size());
        });
    }
}
```
