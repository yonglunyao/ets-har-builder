# 故障排查指南

## 常见问题

### 1. "No source files found" 错误

**症状:**
```
Found 0 source files
No external dependencies found, nothing to do.
```

**原因:**
- 模块路径不正确
- 源文件不在预期的目录结构中

**解决方案:**

检查目录结构是否正确:
```
module-root/
└── src/
    └── main/
        └── ets/
            └── your files.ets
```

确保提供的路径是模块根目录:
```bash
# 错误
mvn exec:java -Dexec.args="D:\code\myproject\mymodule\src\main\ets"

# 正确
mvn exec:java -Dexec.args="D:\code\myproject\mymodule"
```

---

### 2. 生成的 DTS 文件类型不正确

**症状:**
生成的 DTS 文件中某些成员的类型不正确，或使用了 `object`/`any` 等不明确类型导致 ArkTS 编译失败。

**原因:**
- 类型推断基于启发式规则，可能不准确
- 复杂的泛型类型无法正确推断
- **ArkTS 严格要求返回值类型不能是 `object`/`any`**

**解决方案:**

手动编辑生成的 Index.d.ts 文件修正类型:

```typescript
// 原先生成的（使用了不明确的类型）
const Utf8: object;

// 修正为（使用明确的接口类型）
const Utf8: Encoder;

interface Encoder {
    parse(str: string): WordArray;
    stringify(wordArray: WordArray): string;
}

interface WordArray {
    words: number[];
    sigBytes: number;
}
```

或者添加自定义模板到 `DtsGenerator.java`:

```java
private static final Map<String, String> KNOWN_LIBRARY_TEMPLATES = Map.of(
    "@ohos/crypto-js",
    """
    declare global {
        namespace CryptoJS {
            export namespace enc {
                export const Utf8: {
                    parse(str: string): WordArray;
                    stringify(wordArray: WordArray): string;
                };
            }
        }
    }
    """
);
```

---

### 3. 嵌套命名空间未正确生成

**症状:**
成员都生成在根命名空间下，没有嵌套结构。

**原因:**
- 成员访问表达式没有被正确解析
- 中间命名空间没有被创建

**调试步骤:**

1. 启用 debug 日志:
```bash
mvn exec:java -Dexec.mainClass="org.example.harmony.HarDependencyEngine" \
  -Dexec.args="<模块路径>" \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

2. 检查日志中的成员访问解析:
```
Processing member access: baseObject=CryptoJS, fullPath=CryptoJS.enc.Utf8.parse
Added intermediate namespace: enc under CryptoJS
Added intermediate namespace: Utf8 under CryptoJS.enc
Added member: parse to namespace: CryptoJS.enc.Utf8
```

3. 如果看到 `No dependency found`，检查 import 语句是否正确:
```typescript
// 确保使用正确的导入
import { CryptoJS } from '@ohos/crypto-js';  // ✓
import cryptojs from '@ohos/crypto-js';      // ✗ 不支持 default 导入的成员访问解析
```

---

### 4. 某些依赖被跳过

**症状:**
```
Skipping SDK dependency: @kit.AbilityKit
Skipping SDK dependency: @ohos/hvigor-ohos-plugin
```

**原因:**
这些依赖被识别为 HarmonyOS SDK 依赖。

**解决方案:**

如果确实需要为这些依赖生成 DTS:

1. 编辑 `HarDependencyEngine.java`，修改 `SDK_PREFIXES`:
```java
private static final String[] SDK_PREFIXES = {
    "@kit.",
    // "@ohos.",  // 注释掉这一行
    "@hms"
};
```

2. 重新编译并运行:
```bash
mvn package -DskipTests
mvn exec:java -Dexec.mainClass="org.example.harmony.HarDependencyEngine" \
  -Dexec.args="<模块路径>"
```

---

### 5. hvigor 编译失败

**症状:**
DTS 文件生成后，hvigor 编译仍然失败。

**可能原因:**

#### A. DTS 语法错误

检查生成的 Index.d.ts 是否有语法错误:

```bash
# 使用 tsc 检查语法
tsc --noEmit @ohos/crypto-js/Index.d.ts
```

#### B. 类型不匹配

检查源代码中的使用是否与 DTS 声明匹配:

```typescript
// 源代码
const result = CryptoJS.enc.Utf8.parse(data);

// DTS 应该包含（使用明确的类型，禁止 object/any）
declare global {
    namespace CryptoJS {
        namespace enc {
            const Utf8: Encoder;
        }
    }
}

interface Encoder {
    parse(str: string): WordArray;  // 明确的返回值类型
}

interface WordArray {
    words: number[];
    sigBytes: number;
}
```

#### C. 模块路径不正确

检查 oh-package.json5 中的依赖路径:

```json5
{
  "dependencies": {
    "@ohos/crypto-js": "file:../@ohos/crypto-js"  // 路径需要正确
  }
}
```

---

### 6. 相对路径导入被处理

**症状:**
相对路径的 import (如 `import { utils } from './utils'`) 被当作外部依赖处理。

**原因:**
当前版本会处理所有 import，包括相对路径的。

**解决方案:**

相对路径的 import 不应该生成 DTS 文件。如果出现了这个问题，检查:

1. ImportInfo 的 isExternalDependency() 方法:
```java
public boolean isExternalDependency() {
    return !modulePath.startsWith(".") && !modulePath.startsWith("/");
}
```

2. 在 collectDependencies 中过滤:
```java
if (!importInfo.isExternalDependency()) {
    continue;  // 跳过相对路径导入
}
```

---

### 7. 重复的类型声明

**症状:**
生成的 DTS 中有重复的类型声明。

**原因:**
同一个类型被多次添加到 DependencyInfo。

**调试:**

检查 buildNamespaceStructure 中的去重逻辑:

```java
boolean exists = depInfo.getReferencedTypes().stream()
    .anyMatch(t -> t.getName().equals(memberName) &&
                 Objects.equals(t.getParentNamespace(), parentPath));

if (!exists) {
    depInfo.addReferencedType(memberType);
}
```

---

## 调试技巧

### 启用详细日志

```bash
# 启用 debug 级别
mvn exec:java \
  -Dexec.mainClass="org.example.harmony.HarDependencyEngine" \
  -Dexec.args="<模块路径>" \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
  -Dorg.slf4j.simpleLogger.log.org.example.harmony=debug
```

### 单独测试解析器

```java
import org.example.harmony.parser.ArkTSParser;
import java.nio.file.Paths;

public class TestParser {
    public static void main(String[] args) throws Exception {
        ArkTSParser parser = new ArkTSParser();
        var result = parser.parse(Paths.get("path/to/your/file.ets"));

        System.out.println("Imports: " + result.getImports().size());
        result.getImports().forEach(imp -> System.out.println("  - " + imp));

        System.out.println("Member accesses: " + result.getMemberAccesses().size());
        result.getMemberAccesses().forEach(ma -> System.out.println("  - " + ma.getFullPath()));
    }
}
```

### 检查生成的 TypeInfo

```java
import org.example.harmony.scanner.DependencyScanner;
import org.example.harmony.model.DependencyInfo;

public class TestDependencies {
    public static void main(String[] args) throws Exception {
        DependencyScanner scanner = new DependencyScanner();
        var dependencies = scanner.scan("path/to/module");

        dependencies.forEach((modulePath, depInfo) -> {
            System.out.println("\n=== " + modulePath + " ===");
            depInfo.getReferencedTypes().forEach(type -> {
                System.out.println(type.getKind() + " " +
                    type.getParentNamespace() + "." + type.getName());
            });
        });
    }
}
```

## 日志分析

### 正常的日志输出

```
[INFO] Scanning module: D:\code\myproject\mymodule
[INFO] Found 5 source files
[INFO] Found 2 external dependencies
[INFO]   - @ohos/crypto-js: 1 imports, 15 types
[INFO]   - @pura/harmony-utils: 1 imports, 3 types
[INFO] Generating DTS file: .../@ohos/crypto-js/Index.d.ts
[INFO] Generated DTS file with 45 declarations
[INFO] SUCCESS
```

### 异常的日志输出

```
[WARN] Failed to parse file: path/to/file.ets: ...
[INFO] No external dependencies found, nothing to do.
```

这表示没有找到外部依赖，检查:
1. 源文件中是否有 import 语句
2. import 的模块路径是否正确
3. 是否被 SDK 过滤器过滤掉

## 获取帮助

如果问题无法解决:

1. 收集以下信息:
   - 完整的错误日志
   - 源文件的目录结构
   - 示例 import 语句
   - 生成的 DTS 文件内容

2. 检查已知限制:
   - 查看本文档的"限制和已知问题"部分
   - 查看 SKILL.md 中的限制部分

3. 考虑手动修正:
   - 对于简单的类型问题，手动编辑 DTS 文件可能更快
