package org.example.harmony.parser;

import org.apache.commons.lang3.StringUtils;
import org.example.harmony.model.ArkTSParseResult;
import org.example.harmony.model.ImportInfo;
import org.example.harmony.model.MemberAccessInfo;
import org.example.harmony.model.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ArkTS/TypeScript解析器
 * 负责解析.ets和.ts文件，提取import语句和类型定义
 * 支持解析嵌套namespace成员访问
 */
public class ArkTSParser {
    private static final Logger logger = LoggerFactory.getLogger(ArkTSParser.class);

    // 匹配import语句的正则表达式
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^import\\s+(?:(type)\\s+)?(\\{[^}]*\\}|\\*\\s+as\\s+\\w+|\\w+)\\s+from\\s+['\"]([^'\"]+)['\"]"
    );

    /**
     * 匹配成员访问表达式的正则表达式
     * 匹配模式如: CryptoJS.enc.Utf8.parse 或 CryptoJS.mode.CBC
     * 这个正则捕获：
     * - 基础对象名（如 CryptoJS）
     * - 完整路径（如 CryptoJS.enc.Utf8.parse 或 CryptoJS.mode.CBC）
     */
    private static final Pattern MEMBER_ACCESS_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z0-9]*)\\.(?:[a-zA-Z0-9_]+\\.)*([a-zA-Z0-9_]+)(?:\\(|\\b)"
    );

    /**
     * 匹配方法调用的正则（更精确）
     */
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z0-9]*)\\.(?:[a-zA-Z0-9_]+\\.)*([a-zA-Z0-9_]+)\\s*\\("
    );

    /**
     * 匹配属性访问的正则
     */
    private static final Pattern PROPERTY_ACCESS_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z0-9]*)\\.(?:[a-zA-Z0-9_]+\\.)*([a-zA-Z0-9_]+)\\s*(?=[;,)\\]}}]|\\.|\n|$)"
    );

    /**
     * 匹配静态方法调用的正则
     * 例如: CryptoJS.lib.WordArray.random(length) 或 CryptoJS.AES.encrypt(message)
     * 捕获组1: 基础对象（如 CryptoJS）
     * 捕获组2: 完整路径（如 lib.WordArray.random 或 AES.encrypt）
     * 捕获组3: 类/接口名（如 WordArray 或 AES）
     * 捕获组4: 方法名（如 random 或 encrypt）
     *
     * 注意：
     * - 支持两种模式：(1) 有中间路径的（如 lib.WordArray.random）
     *                (2) 没有中间路径但类名大写开头的（如 AES.encrypt）
     * - 排除 enc, mode, pad 这些命名空间下的方法调用
     */
    private static final Pattern STATIC_METHOD_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z0-9]*)\\.(?!enc\\.|mode\\.|pad\\.)((?:[a-zA-Z0-9_]+\\.)*([A-Z][a-zA-Z0-9]*)\\.([a-zA-Z0-9_]+))\\s*\\("
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
            if (StringUtils.isEmpty(line) || line.startsWith("//")) {
                continue;
            }

            // 解析import语句
            parseImport(line, result);

            // 解析export语句
            parseExport(line, result);

            // 解析成员访问表达式
            parseMemberAccess(line, result);
        }

        logger.debug("Parsed {} imports, {} exports, {} member accesses from {}",
                result.getImports().size(), result.getExports().size(),
                result.getMemberAccesses().size(), filePath);

        return result;
    }

    /**
     * 解析成员访问表达式
     * 例如: CryptoJS.enc.Utf8.parse(data)
     *
     * 对于这种表达式，需要记录：
     * - CryptoJS.enc.Utf8.parse (方法调用)
     * - CryptoJS.enc.Utf8 (属性访问，如果后面还有方法调用)
     * - CryptoJS.lib.WordArray.random(length) (静态方法调用)
     */
    private void parseMemberAccess(String line, ArkTSParseResult result) {
        // 用于去重，避免同一个表达式被重复匹配
        Set<String> processedExpressions = new HashSet<>();

        // 首先匹配静态方法调用（如 CryptoJS.lib.WordArray.random）
        Matcher staticMethodMatcher = STATIC_METHOD_PATTERN.matcher(line);
        while (staticMethodMatcher.find()) {
            String baseObject = staticMethodMatcher.group(1);
            String fullPath = staticMethodMatcher.group(2); // lib.WordArray.random
            String className = staticMethodMatcher.group(3);  // WordArray
            String methodName = staticMethodMatcher.group(4);  // random

            // 检查baseObject是否在import列表中
            if (isImportedObject(baseObject, result)) {
                String fullQualifier = baseObject + "." + fullPath;
                // 记录已处理，避免重复匹配
                processedExpressions.add(fullQualifier);

                MemberAccessInfo access = new MemberAccessInfo(
                        baseObject,
                        fullQualifier,
                        methodName,
                        MemberAccessInfo.AccessContext.STATIC_METHOD_CALL,
                        null,  // callSignature
                        className  // className as 6th parameter
                );
                result.addMemberAccess(access);
                logger.info("Found static method call: {} on class {}", fullQualifier, className);
            }
        }

        // 然后匹配普通方法调用
        Matcher methodMatcher = METHOD_CALL_PATTERN.matcher(line);
        while (methodMatcher.find()) {
            String baseObject = methodMatcher.group(1);
            String fullMatch = methodMatcher.group(0);
            String memberName = methodMatcher.group(2);

            // 构建完整路径（包括前面的点链）
            String fullPath = extractFullPath(baseObject, fullMatch, memberName);

            // 检查是否已经被静态方法模式处理过
            if (processedExpressions.contains(fullPath)) {
                continue;
            }

            // 检查baseObject是否在import列表中
            if (isImportedObject(baseObject, result)) {
                // 记录完整的方法调用
                MemberAccessInfo access = new MemberAccessInfo(
                        baseObject,
                        fullPath,
                        memberName,
                        MemberAccessInfo.AccessContext.METHOD_CALL,
                        null
                );
                result.addMemberAccess(access);
                logger.trace("Found method call: {}", fullPath);
            }
        }

        // 匹配属性访问（排除已作为父路径记录的）
        Matcher propMatcher = PROPERTY_ACCESS_PATTERN.matcher(line);
        while (propMatcher.find()) {
            String baseObject = propMatcher.group(1);
            String fullMatch = propMatcher.group(0);
            String memberName = propMatcher.group(2);

            // 构建完整路径
            String fullPath = extractFullPath(baseObject, fullMatch, memberName);

            // 检查是否已经被处理过
            if (processedExpressions.contains(fullPath)) {
                continue;
            }

            // 检查是否已作为某个方法调用的父路径记录
            boolean alreadyRecorded = result.getMemberAccesses().stream()
                    .anyMatch(ma -> ma.getFullPath().equals(fullPath));

            if (!alreadyRecorded && isImportedObject(baseObject, result)) {
                MemberAccessInfo access = new MemberAccessInfo(
                        baseObject,
                        fullPath,
                        memberName,
                        MemberAccessInfo.AccessContext.PROPERTY_ACCESS,
                        null
                );
                result.addMemberAccess(access);
                logger.trace("Found property access: {}", fullPath);
            }
        }
    }

    /**
     * 从完整路径中提取父路径
     * 例如: CryptoJS.enc.Utf8.parse -> CryptoJS.enc.Utf8
     */
    private String extractParentPath(String fullPath) {
        int lastDot = fullPath.lastIndexOf('.');
        if (lastDot > 0) {
            return fullPath.substring(0, lastDot);
        }
        return null;
    }

    /**
     * 从匹配的文本中提取完整的成员访问路径
     */
    private String extractFullPath(String baseObject, String fullMatch, String finalMember) {
        // 移除baseObject前缀
        String remaining = fullMatch.substring(baseObject.length());
        // 移除末尾的括号或空格
        remaining = remaining.replaceAll("\\(.*$", "").replaceAll("\\s*[;,)\\]}]*$", "").trim();
        return baseObject + remaining;
    }

    /**
     * 检查对象是否是从外部模块导入的
     */
    private boolean isImportedObject(String objectName, ArkTSParseResult result) {
        return result.getImports().stream()
                .anyMatch(imp -> imp.getImportedName().equals(objectName) ||
                        "*".equals(imp.getImportedName()) && imp.getLocalAlias().equals(objectName));
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
