package org.example.harmony.scanner;

import org.apache.commons.io.FileUtils;
import org.example.harmony.model.ArkTSParseResult;
import org.example.harmony.model.DependencyInfo;
import org.example.harmony.model.ImportInfo;
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
        }

        return dependencies;
    }

    /**
     * 根据import信息推断类型信息
     */
    private void inferTypeInfo(ImportInfo importInfo, DependencyInfo depInfo, ArkTSParseResult result) {
        String importedName = importInfo.getImportedName();

        if ("*".equals(importedName)) {
            // namespace import，添加通配符声明
            String namespace = importInfo.getLocalAlias();
            TypeInfo typeInfo = new TypeInfo(namespace, "namespace", "declare namespace " + namespace + " {}");
            depInfo.addReferencedType(typeInfo);
        } else {
            // 根据导入名称和文件中的使用情况推断类型
            String kind = inferKind(importedName, result, depInfo);
            TypeInfo typeInfo = new TypeInfo(importedName, kind, generateSignature(importedName, kind));
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
        switch (kind) {
            case "class":
                return "declare class " + name + " {}";
            case "interface":
                return "interface " + name + " {}";
            case "function":
                return "declare function " + name + "(...args: any[]): any;";
            case "type":
                return "type " + name + " = any;";
            case "namespace":
                return "declare namespace " + name + " {}";
            case "const":
                return "declare const " + name + ": any;";
            case "enum":
                return "declare enum " + name + " {}";
            default:
                return "declare var " + name + ": any;";
        }
    }
}
