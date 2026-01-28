package org.example.harmony.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示一个依赖的完整信息
 */
public class DependencyInfo {
    /**
     * 依赖模块路径（如 @pura/harmony-utils）
     */
    private final String modulePath;

    /**
     * 该依赖被引用的类型信息列表
     */
    private final List<TypeInfo> referencedTypes;

    /**
     * 原始import信息
     */
    private final List<ImportInfo> importInfos;

    /**
     * 额外的类型声明（用于添加必要的类型）
     */
    private final Map<String, String> extraDeclarations;

    public DependencyInfo(String modulePath) {
        this.modulePath = modulePath;
        this.referencedTypes = new ArrayList<>();
        this.importInfos = new ArrayList<>();
        this.extraDeclarations = new HashMap<>();
    }

    public String getModulePath() {
        return modulePath;
    }

    public List<TypeInfo> getReferencedTypes() {
        return referencedTypes;
    }

    public List<ImportInfo> getImportInfos() {
        return importInfos;
    }

    public void addReferencedType(TypeInfo type) {
        referencedTypes.add(type);
    }

    public void addImportInfo(ImportInfo importInfo) {
        importInfos.add(importInfo);
    }

    public void addExtraDeclaration(String name, String declaration) {
        extraDeclarations.put(name, declaration);
    }

    public Map<String, String> getExtraDeclarations() {
        return extraDeclarations;
    }

    /**
     * 获取oh-package.json5所需的包名
     * 将@pura/harmony-utils转换为pura_harmony-utils格式
     */
    public String getOhPackageName() {
        if (modulePath.startsWith("@")) {
            // 移除@符号，将/转换为_
            String path = modulePath.substring(1);
            return path.replace("/", "_");
        }
        return modulePath.replace("/", "_");
    }

    @Override
    public String toString() {
        return "DependencyInfo{" +
                "modulePath='" + modulePath + '\'' +
                ", referencedTypes=" + referencedTypes.size() +
                ", importInfos=" + importInfos.size() +
                '}';
    }
}
