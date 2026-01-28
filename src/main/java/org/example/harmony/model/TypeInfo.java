package org.example.harmony.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 表示类型/函数/接口的信息
 * 支持嵌套namespace结构
 */
public class TypeInfo {
    /**
     * 类型名称
     */
    private final String name;

    /**
     * 类型种类: class, interface, type, function, enum, const, namespace
     */
    private final String kind;

    /**
     * 类型签名（用于生成d.ts）
     */
    private final String signature;

    /**
     * 类型参数（泛型）
     */
    private final String typeParameters;

    /**
     * 父namespace路径（对于嵌套类型）
     * 例如: "CryptoJS.enc" 表示 Utf8 是 enc namespace 下的成员
     */
    private final String parentNamespace;

    /**
     * 子类型列表（用于namespace嵌套）
     */
    private final List<TypeInfo> children;

    /**
     * 返回值类型（用于函数和方法）
     */
    private String returnType;

    public TypeInfo(String name, String kind, String signature) {
        this(name, kind, signature, null, null);
    }

    public TypeInfo(String name, String kind, String signature, String typeParameters) {
        this(name, kind, signature, typeParameters, null);
    }

    public TypeInfo(String name, String kind, String signature, String typeParameters, String parentNamespace) {
        this.name = name;
        this.kind = kind;
        this.signature = signature;
        this.typeParameters = typeParameters;
        this.parentNamespace = parentNamespace;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public String getSignature() {
        return signature;
    }

    public String getTypeParameters() {
        return typeParameters;
    }

    public String getParentNamespace() {
        return parentNamespace;
    }

    public List<TypeInfo> getChildren() {
        return children;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /**
     * 添加子类型（用于namespace）
     */
    public void addChild(TypeInfo child) {
        children.add(child);
    }

    /**
     * 获取完整路径（包括namespace）
     * 例如: "CryptoJS.enc.Utf8"
     */
    public String getFullPath() {
        if (parentNamespace != null && !parentNamespace.isEmpty()) {
            return parentNamespace + "." + name;
        }
        return name;
    }

    /**
     * 判断是否为顶层类型（没有父namespace）
     */
    public boolean isTopLevel() {
        return parentNamespace == null || parentNamespace.isEmpty();
    }

    /**
     * 判断是否有子类型
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeInfo typeInfo = (TypeInfo) o;
        return Objects.equals(name, typeInfo.name) &&
                Objects.equals(kind, typeInfo.kind) &&
                Objects.equals(parentNamespace, typeInfo.parentNamespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind, parentNamespace);
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "name='" + name + '\'' +
                ", kind='" + kind + '\'' +
                ", parentNamespace='" + parentNamespace + '\'' +
                ", children=" + children.size() +
                '}';
    }
}
