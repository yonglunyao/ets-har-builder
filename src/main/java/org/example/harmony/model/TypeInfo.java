package org.example.harmony.model;

/**
 * 表示类型/函数/接口的信息
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

    public TypeInfo(String name, String kind, String signature) {
        this(name, kind, signature, null);
    }

    public TypeInfo(String name, String kind, String signature, String typeParameters) {
        this.name = name;
        this.kind = kind;
        this.signature = signature;
        this.typeParameters = typeParameters;
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

    @Override
    public String toString() {
        return "TypeInfo{" +
                "name='" + name + '\'' +
                ", kind='" + kind + '\'' +
                ", signature='" + signature + '\'' +
                ", typeParameters='" + typeParameters + '\'' +
                '}';
    }
}
