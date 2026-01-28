package org.example.harmony.parser;

import org.apache.commons.lang3.StringUtils;
import org.example.harmony.model.ArkTSParseResult;
import org.example.harmony.model.ImportInfo;
import org.example.harmony.model.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ArkTS/TypeScript解析器
 * 负责解析.ets和.ts文件，提取import语句和类型定义
 */
public class ArkTSParser {
    private static final Logger logger = LoggerFactory.getLogger(ArkTSParser.class);

    // 匹配import语句的正则表达式
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^import\\s+(?:(type)\\s+)?(\\{[^}]*\\}|\\*\\s+as\\s+\\w+|\\w+)\\s+from\\s+['\"]([^'\"]+)['\"]"
    );

    // 匹配export语句
    private static final Pattern EXPORT_CLASS_PATTERN = Pattern.compile(
            "^export\\s+(?:default\\s+)?class\\s+(\\w+)(?:\\s*<[^>]*>)?"
    );

    private static final Pattern EXPORT_INTERFACE_PATTERN = Pattern.compile(
            "^export\\s+(?:default\\s+)?interface\\s+(\\w+)(?:\\s*<[^>]*>)?"
    );

    private static final Pattern EXPORT_TYPE_PATTERN = Pattern.compile(
            "^export\\s+(?:default\\s+)?type\\s+(\\w+)\\s*="
    );

    private static final Pattern EXPORT_FUNCTION_PATTERN = Pattern.compile(
            "^export\\s+(?:default\\s+)?(?:async\\s+)?function\\s+(\\w+)\\s*\\("
    );

    private static final Pattern EXPORT_CONST_PATTERN = Pattern.compile(
            "^export\\s+(?:default\\s+)?const\\s+(\\w+)\\s*[=:]"
    );

    private static final Pattern EXPORT_ENUM_PATTERN = Pattern.compile(
            "^export\\s+(?:default\\s+)?enum\\s+(\\w+)"
    );

    // 匹配@装饰器结构体导出
    private static final Pattern EXPORT_STRUCT_PATTERN = Pattern.compile(
            "@Component\\s*\\n\\s*export\\s+struct\\s+(\\w+)"
    );

    /**
     * 解析ArkTS/TypeScript文件
     */
    public ArkTSParseResult parse(Path filePath) throws IOException {
        logger.debug("Parsing file: {}", filePath);

        ArkTSParseResult result = new ArkTSParseResult(filePath.toString());
        List<String> lines = Files.readAllLines(filePath);

        for (String line : lines) {
            line = line.trim();
            if (StringUtils.isEmpty(line)) {
                continue;
            }

            // 解析import语句
            parseImport(line, result);

            // 解析export语句
            parseExport(line, result);
        }

        logger.debug("Parsed {} imports and {} exports from {}",
                result.getImports().size(), result.getExports().size(), filePath);

        return result;
    }

    /**
     * 解析import语句
     */
    private void parseImport(String line, ArkTSParseResult result) {
        Matcher matcher = IMPORT_PATTERN.matcher(line);
        if (!matcher.find()) {
            return;
        }

        boolean isTypeImport = matcher.group(1) != null;
        String importsStr = matcher.group(2);
        String modulePath = matcher.group(3);

        // 解析导入的内容
        if (importsStr.startsWith("{")) {
            // named imports: import { A, B, C as D } from 'module'
            String content = importsStr.substring(1, importsStr.length() - 1);
            String[] parts = content.split(",");

            for (String part : parts) {
                part = part.trim();
                if (StringUtils.isEmpty(part)) {
                    continue;
                }

                String importedName;
                String localAlias = null;

                if (part.contains(" as ")) {
                    String[] aliasParts = part.split(" as ");
                    importedName = aliasParts[0].trim();
                    localAlias = aliasParts[1].trim();
                } else {
                    importedName = part;
                }

                ImportInfo importInfo = new ImportInfo(modulePath, importedName, localAlias, isTypeImport);
                result.addImport(importInfo);
                logger.trace("Found import: {}", importInfo);
            }
        } else if (importsStr.startsWith("*")) {
            // namespace import: import * as ns from 'module'
            String namespace = importsStr.substring(importsStr.indexOf("as") + 2).trim();
            ImportInfo importInfo = new ImportInfo(modulePath, "*", namespace, isTypeImport);
            result.addImport(importInfo);
            logger.trace("Found namespace import: {} as {}", modulePath, namespace);
        } else {
            // default import: import A from 'module'
            ImportInfo importInfo = new ImportInfo(modulePath, importsStr, null, isTypeImport);
            result.addImport(importInfo);
            logger.trace("Found default import: {} from {}", importsStr, modulePath);
        }
    }

    /**
     * 解析export语句
     */
    private void parseExport(String line, ArkTSParseResult result) {
        Matcher matcher;

        if ((matcher = EXPORT_CLASS_PATTERN.matcher(line)).find()) {
            result.addExport(new TypeInfo(matcher.group(1), "class", generateClassSignature(line)));
        } else if ((matcher = EXPORT_INTERFACE_PATTERN.matcher(line)).find()) {
            result.addExport(new TypeInfo(matcher.group(1), "interface", generateInterfaceSignature(line)));
        } else if ((matcher = EXPORT_TYPE_PATTERN.matcher(line)).find()) {
            result.addExport(new TypeInfo(matcher.group(1), "type", generateTypeAliasSignature(line)));
        } else if ((matcher = EXPORT_FUNCTION_PATTERN.matcher(line)).find()) {
            result.addExport(new TypeInfo(matcher.group(1), "function", generateFunctionSignature(line)));
        } else if ((matcher = EXPORT_CONST_PATTERN.matcher(line)).find()) {
            result.addExport(new TypeInfo(matcher.group(1), "const", generateConstSignature(line)));
        } else if ((matcher = EXPORT_ENUM_PATTERN.matcher(line)).find()) {
            result.addExport(new TypeInfo(matcher.group(1), "enum", "enum " + matcher.group(1) + " {}"));
        } else if ((matcher = EXPORT_STRUCT_PATTERN.matcher(line)).find()) {
            result.addExport(new TypeInfo(matcher.group(1), "struct", generateStructSignature(line)));
        }
    }

    private String generateClassSignature(String line) {
        // 简单生成class签名，实际可以更复杂
        return line.replaceAll("\\s+", " ").trim() + " {}";
    }

    private String generateInterfaceSignature(String line) {
        return line.replaceAll("\\s+", " ").trim() + " {}";
    }

    private String generateTypeAliasSignature(String line) {
        return line.replaceAll("\\s+", " ").trim() + ";";
    }

    private String generateFunctionSignature(String line) {
        // 简化处理，实际应该解析完整函数签名
        return line.replaceAll("\\s+", " ").trim() + ";";
    }

    private String generateConstSignature(String line) {
        return line.replaceAll("\\s+", " ").trim() + ";";
    }

    private String generateStructSignature(String line) {
        // HarmonyOS @Component struct
        String name = line.replaceAll(".*struct\\s+(\\w+).*", "$1");
        return "@Component struct " + name + " {}";
    }
}
