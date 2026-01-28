package org.example.harmony.model;

import java.util.Objects;

/**
 * 表示import语句的信息
 */
public class ImportInfo {
    /**
     * import语句的完整路径
     * 例如: @pura/harmony-utils, @kit.AbilityKit
     */
    private final String modulePath;

    /**
     * 导入的具体内容
     * 例如: PermissionUtil, Permissions
     */
    private final String importedName;

    /**
     * 本地别名（如果有as关键字）
     */
    private final String localAlias;

    /**
     * 是否为类型导入
     */
    private final boolean isTypeImport;

    public ImportInfo(String modulePath, String importedName, String localAlias, boolean isTypeImport) {
        this.modulePath = modulePath;
        this.importedName = importedName;
        this.localAlias = localAlias;
        this.isTypeImport = isTypeImport;
    }

    public String getModulePath() {
        return modulePath;
    }

    public String getImportedName() {
        return importedName;
    }

    public String getLocalAlias() {
        return localAlias;
    }

    public boolean isTypeImport() {
        return isTypeImport;
    }

    /**
     * 判断是否为外部依赖（非相对路径）
     */
    public boolean isExternalDependency() {
        return modulePath.startsWith("@") || !modulePath.startsWith(".");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportInfo that = (ImportInfo) o;
        return isTypeImport == that.isTypeImport &&
                Objects.equals(modulePath, that.modulePath) &&
                Objects.equals(importedName, that.importedName) &&
                Objects.equals(localAlias, that.localAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modulePath, importedName, localAlias, isTypeImport);
    }

    @Override
    public String toString() {
        return "ImportInfo{" +
                "modulePath='" + modulePath + '\'' +
                ", importedName='" + importedName + '\'' +
                ", localAlias='" + localAlias + '\'' +
                ", isTypeImport=" + isTypeImport +
                '}';
    }
}
