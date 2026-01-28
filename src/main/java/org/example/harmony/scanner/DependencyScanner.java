package org.example.harmony.scanner;

import org.apache.commons.io.FileUtils;
import org.example.harmony.model.ArkTSParseResult;
import org.example.harmony.model.DependencyInfo;
import org.example.harmony.model.ImportInfo;
import org.example.harmony.model.InterfaceMethodInfo;
import org.example.harmony.model.MemberAccessInfo;
import org.example.harmony.model.TypeInfo;
import org.example.harmony.parser.ArkTSParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 依赖扫描器
 * 负责扫描HarmonyOS HAR模块，收集所有外部依赖引用
 * 支持解析嵌套namespace成员访问
 */
public class DependencyScanner {
    private static final Logger logger = LoggerFactory.getLogger(DependencyScanner.class);

    private final ArkTSParser parser;

    public DependencyScanner() {
        this.parser = new ArkTSParser();
    }

    /**
     * 扫描HAR模块目录，收集所有外部依赖
     *
     * @param modulePath HAR模块根目录
     * @return 依赖信息列表，按模块路径分组
     */
    public Map<String, DependencyInfo> scan(String modulePath) throws IOException {
        logger.info("Scanning module: {}", modulePath);

        // 查找所有.ets和.ts文件
        List<Path> sourceFiles = findSourceFiles(modulePath);
        logger.info("Found {} source files", sourceFiles.size());

        // 解析所有源文件
        List<ArkTSParseResult> parseResults = new ArrayList<>();
        for (Path file : sourceFiles) {
            try {
                ArkTSParseResult result = parser.parse(file);
                parseResults.add(result);
            } catch (Exception e) {
                logger.warn("Failed to parse file {}: {}", file, e.getMessage());
            }
        }

        // 收集所有外部依赖
        Map<String, DependencyInfo> dependencies = collectDependencies(parseResults);
        logger.info("Found {} external dependencies", dependencies.size());

        for (DependencyInfo dep : dependencies.values()) {
            logger.info("  - {}: {} imports, {} types",
                    dep.getModulePath(),
                    dep.getImportInfos().size(),
                    dep.getReferencedTypes().size());
        }

        return dependencies;
    }

    /**
     * 查找所有源文件（.ets, .ts）
     */
    private List<Path> findSourceFiles(String modulePath) throws IOException {
        List<Path> files = new ArrayList<>();

        Path srcMainPath = Paths.get(modulePath, "src", "main");
        if (Files.exists(srcMainPath)) {
            try (Stream<Path> paths = Files.walk(srcMainPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".ets") || p.toString().endsWith(".ts"))
                        .forEach(files::add);
            }
        }

        // 检查根目录下的Index.ets等文件
        Path rootPath = Paths.get(modulePath);
        if (Files.exists(rootPath)) {
            try (Stream<Path> paths = Files.walk(rootPath, 1)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".ets") || p.toString().endsWith(".ts"))
                        .forEach(files::add);
            }
        }

        return files;
    }

    /**
     * 从解析结果中收集所有外部依赖
     */
    private Map<String, DependencyInfo> collectDependencies(List<ArkTSParseResult> parseResults) {
        Map<String, DependencyInfo> dependencies = new HashMap<>();

        for (ArkTSParseResult result : parseResults) {
            for (ImportInfo importInfo : result.getImports()) {
                // 只处理外部依赖（非相对路径）
                if (!importInfo.isExternalDependency()) {
                    continue;
                }

                String modulePath = importInfo.getModulePath();

                // 获取或创建DependencyInfo
                DependencyInfo depInfo = dependencies.computeIfAbsent(
                        modulePath,
                        DependencyInfo::new
                );

                depInfo.addImportInfo(importInfo);

                // 根据导入的名称推断类型信息
                inferTypeInfo(importInfo, depInfo, result);
            }

            // 处理成员访问表达式（构建嵌套namespace）
            processMemberAccesses(result, dependencies);
        }

        return dependencies;
    }

    /**
     * 处理成员访问表达式，构建嵌套namespace结构
     */
    private void processMemberAccesses(ArkTSParseResult result, Map<String, DependencyInfo> dependencies) {
        logger.debug("Processing {} member accesses for file: {}", result.getMemberAccesses().size(), result.getFilePath());

        for (MemberAccessInfo memberAccess : result.getMemberAccesses()) {
            String baseObject = memberAccess.getBaseObject();
            String fullPath = memberAccess.getFullPath();

            logger.debug("  Processing member access: baseObject={}, fullPath={}", baseObject, fullPath);

            // 找到对应的DependencyInfo
            DependencyInfo depInfo = findDependencyInfoByImport(result, baseObject, dependencies);
            if (depInfo == null) {
                logger.debug("    No dependency found for baseObject: {}", baseObject);
                continue;
            }

            logger.debug("    Found dependency: {}", depInfo.getModulePath());

            // 处理静态方法调用（如 WordArray.random）
            if (memberAccess.isStaticMethodCall()) {
                processStaticMethodCall(depInfo, memberAccess);
                continue;
            }

            // 解析路径，构建嵌套namespace结构
            buildNamespaceStructure(depInfo, fullPath, memberAccess);
        }
    }

    /**
     * 处理静态方法调用
     * 例如: CryptoJS.lib.WordArray.random(length)
     * - className: "WordArray"
     * - methodName: "random"
     */
    private void processStaticMethodCall(DependencyInfo depInfo, MemberAccessInfo memberAccess) {
        String className = memberAccess.getClassName();
        String methodName = memberAccess.getMemberName();

        logger.debug("    Processing static method call: {}.{}", className, methodName);

        // 推断返回值类型
        String returnType = inferStaticMethodReturnType(className, methodName);

        // 创建接口方法信息
        InterfaceMethodInfo methodInfo = new InterfaceMethodInfo(
                methodName,
                true,  // isStatic
                returnType,
                className,
                memberAccess.getFullPath()
        );

        // 添加参数（根据方法名推断）
        inferMethodParameters(className, methodName, methodInfo);

        // 添加到依赖信息
        depInfo.addInterfaceMethod(methodInfo);

        // 确保类本身也被记录为一个类型
        boolean classExists = depInfo.getReferencedTypes().stream()
                .anyMatch(t -> t.getName().equals(className) && t.isTopLevel());

        if (!classExists) {
            TypeInfo classType = new TypeInfo(className, "interface", null, null, null);
            depInfo.addReferencedType(classType);
            logger.debug("    Added interface type: {}", className);
        }

        logger.info("    Added static method: {}.{} -> {} (total methods for {}: {})",
            className, methodName, returnType, className,
            depInfo.getMethodsForInterface(className).size());
    }

    /**
     * 推断静态方法的返回值类型
     * 使用通用规则推断，不硬编码特定库的类型
     */
    private String inferStaticMethodReturnType(String className, String methodName) {
        // 基于方法名的通用推断规则

        // 工厂方法通常返回类本身的类型
        if (methodName.equals("create") || methodName.equals("from")) {
            return className;  // 返回类本身的类型
        }

        // 生成器方法通常返回类本身的类型
        if (methodName.equals("random") || methodName.equals("generate")) {
            return className;  // 返回类本身的类型
        }

        // 默认：基于类名推断返回类型
        // 如果类名以Array结尾，返回该类；否则使用通用类型
        if (className.endsWith("Array") || className.endsWith("Config") ||
            className.endsWith("Params") || className.endsWith("Options")) {
            return className;
        }

        // 对于其他类，默认返回类本身
        return className;
    }

    /**
     * 推断方法参数
     * 使用通用规则推断，不硬编码特定库的类型
     */
    private void inferMethodParameters(String className, String methodName, InterfaceMethodInfo methodInfo) {
        // 基于方法名的通用推断规则

        // create/from 方法：通常接受数据或大小参数
        if (methodName.equals("create") || methodName.equals("from")) {
            if (className.endsWith("Array") || className.endsWith("List")) {
                methodInfo.addParameter("size", "number");
            } else {
                methodInfo.addParameter("data", "unknown");
            }
            return;
        }

        // random/generate 方法：通常接受大小参数
        if (methodName.equals("random") || methodName.equals("generate")) {
            methodInfo.addParameter("size", "number");
            return;
        }

        // parse 方法：通常接受字符串参数
        if (methodName.equals("parse")) {
            methodInfo.addParameter("input", "string");
            return;
        }

        // 其他方法：添加通用参数
        methodInfo.addParameter("args", "unknown");
    }

    /**
     * 根据import名称查找对应的DependencyInfo
     */
    private DependencyInfo findDependencyInfoByImport(ArkTSParseResult result, String importName,
                                                       Map<String, DependencyInfo> dependencies) {
        return result.getImports().stream()
                .filter(imp -> imp.getImportedName().equals(importName) ||
                        ("*".equals(imp.getImportedName()) && imp.getLocalAlias().equals(importName)))
                .findFirst()
                .map(imp -> dependencies.get(imp.getModulePath()))
                .orElse(null);
    }

    /**
     * 构建嵌套namespace结构
     * 例如: CryptoJS.enc.Utf8.parse 会创建:
     * - namespace enc (在 CryptoJS 下)
     * - const Utf8 (在 CryptoJS.enc 下)
     * - function parse (在 CryptoJS.enc.Utf8 下)
     *
     * DtsGenerator会根据parentNamespace自动生成namespace声明
     */
    private void buildNamespaceStructure(DependencyInfo depInfo, String fullPath, MemberAccessInfo memberAccess) {
        String[] parts = fullPath.split("\\.");

        logger.info("  buildNamespaceStructure: fullPath={}, parts.length={}, context={}",
                fullPath, parts.length, memberAccess.getContext());

        if (parts.length < 2) {
            return;
        }

        // 为所有中间路径创建namespace/const条目
        // 例如: ["CryptoJS", "enc", "Utf8", "parse"]
        // 我们需要创建: enc, Utf8 (以及它们的父路径关系)
        for (int i = 1; i < parts.length - 1; i++) {
            String namespaceName = parts[i];
            String parentPath = String.join(".", Arrays.copyOf(parts, i));

            // 检查是否已存在此namespace
            boolean exists = depInfo.getReferencedTypes().stream()
                    .anyMatch(t -> t.getName().equals(namespaceName) &&
                                     Objects.equals(t.getParentNamespace(), parentPath));

            if (!exists) {
                // 创建中间namespace条目（使用const类型，稍后会被识别为有子成员而生成namespace）
                TypeInfo namespaceType = new TypeInfo(
                        namespaceName,
                        "const",
                        null,
                        null,
                        parentPath
                );
                depInfo.addReferencedType(namespaceType);
                logger.trace("Added intermediate namespace: {} under {}", namespaceName, parentPath);
            }
        }

        // 最后一个部分是成员（方法、属性等）
        String memberName = parts[parts.length - 1];
        String parentPath = String.join(".", Arrays.copyOf(parts, parts.length - 1));

        // 检查是否已存在此成员
        // 注意：需要区分顶层接口（kind=interface, parentNs=null）和 namespace 中的 const（kind=const）
        // 这里只检查同类型的同位置成员，避免重复添加
        String kind = inferMemberKind(memberAccess);
        boolean exists = depInfo.getReferencedTypes().stream()
                .filter(t -> t.getKind().equals(kind))
                .anyMatch(t -> t.getName().equals(memberName) &&
                                 Objects.equals(t.getParentNamespace(), parentPath));

        if (!exists) {
            TypeInfo memberType = new TypeInfo(
                    memberName,
                    kind,
                    null,
                    null,
                    parentPath
            );

            // 设置返回值类型
            if (memberAccess.getContext() == MemberAccessInfo.AccessContext.METHOD_CALL) {
                memberType.setReturnType(inferMethodReturnType(memberName));
            }

            depInfo.addReferencedType(memberType);
            logger.trace("Added member: {} to namespace: {}", memberName, parentPath);
        }

        // 为接口添加实例方法（仅当不是根命名空间下的直接方法调用时）
        // 例如: CryptoJS.enc.Utf8.parse -> 为 Utf8 接口添加 parse 方法
        // 但是: CryptoJS.MD5 -> 不添加实例方法（因为 MD5 是 CryptoJS 的函数，不是接口方法）
        //      CryptoJS.AES.encrypt -> 不添加实例方法（因为 AES.encrypt 是静态方法）
        if (parts.length >= 3) {
            // 例如: ["CryptoJS", "enc", "Utf8", "parse"]
            // 接口名是倒数第二个: Utf8
            // 方法名是最后一个: parse
            String interfaceName = parts[parts.length - 2];
            String methodName = parts[parts.length - 1];

            // 只有当是方法调用时才添加
            if (memberAccess.getContext() == MemberAccessInfo.AccessContext.METHOD_CALL) {
                // 推断返回值类型
                String returnType = inferInstanceMethodReturnType(interfaceName, methodName);

                // 创建接口方法信息（非静态）
                InterfaceMethodInfo methodInfo = new InterfaceMethodInfo(
                        methodName,
                        false,  // 不是静态方法
                        returnType,
                        interfaceName,
                        fullPath
                );

                // 推断参数
                inferInstanceMethodParameters(interfaceName, methodName, methodInfo);

                // 添加到依赖信息
                depInfo.addInterfaceMethod(methodInfo);

                logger.info("    Added instance method: {}.{} -> {} (total methods for {}: {})",
                        interfaceName, methodName, returnType, interfaceName,
                        depInfo.getMethodsForInterface(interfaceName).size());
            }
        }
    }

    /**
     * 推断实例方法的返回值类型
     */
    private String inferInstanceMethodReturnType(String interfaceName, String methodName) {
        // parse 方法通常返回某种对象类型
        if (methodName.equals("parse")) {
            // 返回 unknown 类型，因为 parse 返回的是 WordArray 或类似的对象
            return "unknown";
        }

        // stringify 方法返回字符串
        if (methodName.equals("stringify") || methodName.equals("toString")) {
            return "string";
        }

        // 默认返回 unknown
        return "unknown";
    }

    /**
     * 推断实例方法的参数
     */
    private void inferInstanceMethodParameters(String interfaceName, String methodName, InterfaceMethodInfo methodInfo) {
        // parse 方法：通常接受字符串参数
        if (methodName.equals("parse")) {
            methodInfo.addParameter("input", "string");
            return;
        }

        // stringify/toString 方法：可选的编码器参数
        if (methodName.equals("stringify") || methodName.equals("toString")) {
            methodInfo.addParameter("encoder", "unknown");
            return;
        }

        // 其他方法：通用参数
        methodInfo.addParameter("args", "unknown");
    }

    /**
     * 查找或创建namespace
     * 注意：不再需要此方法，因为我们不创建namespace类型的TypeInfo
     * DtsGenerator会根据parentNamespace自动生成namespace声明
     */
    @Deprecated
    private TypeInfo findOrCreateNamespace(DependencyInfo depInfo, String name, String parentNamespace) {
        // 此方法已废弃，不再使用
        return null;
    }

    /**
     * 推断成员类型
     * 根据成员名称和上下文推断
     */
    private String inferMemberKind(MemberAccessInfo memberAccess) {
        String memberName = memberAccess.getMemberName();

        // 如果是方法调用，返回function
        if (memberAccess.getContext() == MemberAccessInfo.AccessContext.METHOD_CALL) {
            return "function";
        }

        // 如果是属性访问，可能是：
        // 1. 简单const（如 CBC, Pkcs7）
        // 2. 有方法的对象（如 Utf8, Base64）

        // 检查是否已存在该成员的其他类型（如函数）
        // 这需要传递 DependencyInfo 来检查，但我们可以根据名称推断
        if (isObjectLikeType(memberName)) {
            return "const";  // 有方法的对象，稍后会生成为namespace
        } else {
            return "const";
        }
    }

    /**
     * 判断是否是对象类型（可能有方法的对象）
     * 使用通用规则推断，不硬编码特定库的类型
     */
    private boolean isObjectLikeType(String name) {
        // 基于命名约定推断：首字母大写的常量可能是对象类型
        // 例如：Utf8, Hex, Base64, Latin1, Encoder, Config, Options
        return Character.isUpperCase(name.charAt(0));
    }

    /**
     * 推断方法返回值类型
     * 使用通用规则推断，不硬编码特定库的类型
     * 必须返回明确的类型，禁止使用object/any
     */
    private String inferMethodReturnType(String methodName) {
        // 基于方法名的通用推断规则

        // parse 方法：通常返回某种解析后的类型
        if (methodName.equals("parse")) {
            return "unknown";
        }

        // stringify/toString 方法：通常返回string
        if (methodName.equals("stringify") || methodName.equals("toString")) {
            return "string";
        }

        // 其他方法：无法确定返回类型，使用unknown
        // 注意：禁止使用 any 或 object
        return "unknown";
    }

    /**
     * 根据import信息推断类型信息
     */
    private void inferTypeInfo(ImportInfo importInfo, DependencyInfo depInfo, ArkTSParseResult result) {
        String importedName = importInfo.getImportedName();

        if ("*".equals(importedName)) {
            // namespace import，添加根namespace声明
            String namespace = importInfo.getLocalAlias();
            TypeInfo typeInfo = new TypeInfo(namespace, "namespace", "declare namespace " + namespace + " {}", null, null);
            depInfo.addReferencedType(typeInfo);
        } else {
            // 根据导入名称和文件中的使用情况推断类型
            String kind = inferKind(importedName, result, depInfo);
            TypeInfo typeInfo = new TypeInfo(importedName, kind, generateSignature(importedName, kind), null, null);
            depInfo.addReferencedType(typeInfo);
        }
    }

    /**
     * 推断类型的种类（class, interface, function, const等）
     */
    private String inferKind(String name, ArkTSParseResult result, DependencyInfo depInfo) {
        // 尝试从已知的模块模式推断
        String modulePath = depInfo.getModulePath();

        // @kit.AbilityKit 等 HarmonyOS SDK 模块
        if (modulePath.startsWith("@kit.")) {
            return inferKitKind(name);
        }

        // 默认推断为 class
        if (Character.isUpperCase(name.charAt(0))) {
            return "class";
        }

        return "function";
    }

    /**
     * 推断HarmonyOS SDK模块的类型
     */
    private String inferKitKind(String name) {
        // 常见的HarmonyOS SDK类型
        if (name.endsWith("Manager") || name.endsWith("Controller")) {
            return "class";
        }
        if (name.equals("Permissions") || name.equals("abilityAccessCtrl")) {
            return "type";
        }
        return "interface";
    }

    /**
     * 生成类型签名
     */
    private String generateSignature(String name, String kind) {
        return switch (kind) {
            case "class" -> "declare class " + name + " {}";
            case "interface" -> "interface " + name + " {}";
            case "function" -> "declare function " + name + "(...args: any[]): any;";
            case "type" -> "type " + name + " = any;";
            case "namespace" -> "declare namespace " + name + " {}";
            case "const" -> "declare const " + name + ": any;";
            case "enum" -> "declare enum " + name + " {}";
            default -> "declare var " + name + ": any;";
        };
    }
}
