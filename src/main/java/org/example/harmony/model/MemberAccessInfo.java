package org.example.harmony.model;

import java.util.Objects;

/**
 * 表示成员访问表达式的信息
 * 例如: CryptoJS.enc.Utf8.parse
 */
public class MemberAccessInfo {
    /**
     * 基础对象名（通常是import的名称）
     * 例如: "CryptoJS"
     */
    private final String baseObject;

    /**
     * 完整的成员访问路径
     * 例如: "CryptoJS.enc.Utf8"
     */
    private final String fullPath;

    /**
     * 访问的成员名称
     * 例如: "parse" (对于 CryptoJS.enc.Utf8.parse)
     * 或者 "Utf8" (对于 CryptoJS.enc.Utf8)
     */
    private final String memberName;

    /**
     * 访问的上下文信息（方法调用、属性访问等）
     */
    private final AccessContext context;

    /**
     * 调用参数信息（如果是方法调用）
     */
    private final String callSignature;

    /**
     * 类名（对于静态方法调用）
     * 例如: "WordArray" (对于 CryptoJS.lib.WordArray.random)
     */
    private final String className;

    public MemberAccessInfo(String baseObject, String fullPath, String memberName, AccessContext context, String callSignature) {
        this(baseObject, fullPath, memberName, context, callSignature, null);
    }

    public MemberAccessInfo(String baseObject, String fullPath, String memberName, AccessContext context, String callSignature, String className) {
        this.baseObject = baseObject;
        this.fullPath = fullPath;
        this.memberName = memberName;
        this.context = context;
        this.callSignature = callSignature;
        this.className = className;
    }

    public String getBaseObject() {
        return baseObject;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getMemberName() {
        return memberName;
    }

    public AccessContext getContext() {
        return context;
    }

    public String getCallSignature() {
        return callSignature;
    }

    public String getClassName() {
        return className;
    }

    /**
     * 判断是否是静态方法调用
     */
    public boolean isStaticMethodCall() {
        return context == AccessContext.STATIC_METHOD_CALL && className != null;
    }

    /**
     * 获取namespace路径（不包括最后的成员名）
     * 例如: "CryptoJS.enc" (对于 CryptoJS.enc.Utf8)
     */
    public String getNamespacePath() {
        int lastDot = fullPath.lastIndexOf('.');
        if (lastDot > 0) {
            return fullPath.substring(0, lastDot);
        }
        return baseObject;
    }

    /**
     * 访问上下文类型
     */
    public enum AccessContext {
        /** 方法调用 */
        METHOD_CALL,
        /** 静态方法调用 (如 WordArray.random()) */
        STATIC_METHOD_CALL,
        /** 属性访问 */
        PROPERTY_ACCESS,
        /** 构造函数调用 */
        CONSTRUCTOR_CALL,
        /** 类型引用 */
        TYPE_REFERENCE,
        /** 未知 */
        UNKNOWN
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberAccessInfo that = (MemberAccessInfo) o;
        return Objects.equals(baseObject, that.baseObject) &&
                Objects.equals(fullPath, that.fullPath) &&
                Objects.equals(memberName, that.memberName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseObject, fullPath, memberName);
    }

    @Override
    public String toString() {
        return "MemberAccessInfo{" +
                "baseObject='" + baseObject + '\'' +
                ", fullPath='" + fullPath + '\'' +
                ", memberName='" + memberName + '\'' +
                ", context=" + context +
                ", className='" + className + '\'' +
                '}';
    }
}
