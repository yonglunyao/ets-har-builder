package org.example.harmony.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ArkTS文件解析结果
 */
public class ArkTSParseResult {
    /**
     * 文件路径
     */
    private final String filePath;

    /**
     * 所有import语句
     */
    private final List<ImportInfo> imports;

    /**
     * 所有导出的类型/函数/接口
     */
    private final List<TypeInfo> exports;

    /**
     * 文件中引用的所有符号（用于后续分析）
     */
    private final Map<String, Integer> referencedSymbols;

    public ArkTSParseResult(String filePath) {
        this.filePath = filePath;
        this.imports = new ArrayList<>();
        this.exports = new ArrayList<>();
        this.referencedSymbols = new HashMap<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public List<ImportInfo> getImports() {
        return imports;
    }

    public List<TypeInfo> getExports() {
        return exports;
    }

    public Map<String, Integer> getReferencedSymbols() {
        return referencedSymbols;
    }

    public void addImport(ImportInfo importInfo) {
        imports.add(importInfo);
    }

    public void addExport(TypeInfo exportInfo) {
        exports.add(exportInfo);
    }

    public void addReference(String symbol) {
        referencedSymbols.put(symbol, referencedSymbols.getOrDefault(symbol, 0) + 1);
    }

    public boolean hasExport(String name) {
        return exports.stream().anyMatch(e -> e.getName().equals(name));
    }
}
