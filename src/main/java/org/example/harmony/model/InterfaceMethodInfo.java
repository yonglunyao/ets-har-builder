package org.example.harmony.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 接口方法信息
 * 用于跟踪接口/类的方法定义
 */
public class InterfaceMethodInfo {
    /**
     * 方法名称
     */
    private final String methodName;

    /**
     * 方法是否是静态方法
     */
    private final boolean isStatic;

    /**
     * 参数列表
     */
    private final List<MethodParameter> parameters;

    /**
     * 返回值类型
     */
    private final String returnType;

    /**
     * 定义该方法的接口/类名
     */
    private final String interfaceName;

    /**
     * 方法定义的完整路径（如 CryptoJS.lib.WordArray.random）
     */
    private final String fullQualifier;

    public InterfaceMethodInfo(String methodName, boolean isStatic, String returnType,
                               String interfaceName, String fullQualifier) {
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.returnType = returnType;
        this.interfaceName = interfaceName;
        this.fullQualifier = fullQualifier;
        this.parameters = new ArrayList<>();
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getFullQualifier() {
        return fullQualifier;
    }

    public List<MethodParameter> getParameters() {
        return parameters;
    }

    public void addParameter(MethodParameter parameter) {
        parameters.add(parameter);
    }

    public void addParameter(String name, String type) {
        parameters.add(new MethodParameter(name, type));
    }

    /**
     * 生成方法签名
     */
    public String generateSignature() {
        StringBuilder sb = new StringBuilder();

        if (isStatic) {
            sb.append("static ");
        }

        sb.append(methodName).append("(");

        // 生成参数列表
        for (int i = 0; i < parameters.size(); i++) {
            MethodParameter param = parameters.get(i);
            sb.append(param.getName()).append(": ").append(param.getType());
            if (i < parameters.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append("): ").append(returnType);

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterfaceMethodInfo that = (InterfaceMethodInfo) o;
        return Objects.equals(methodName, that.methodName) &&
                Objects.equals(interfaceName, that.interfaceName) &&
                isStatic == that.isStatic;
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, interfaceName, isStatic);
    }

    @Override
    public String toString() {
        return "InterfaceMethodInfo{" +
                "methodName='" + methodName + '\'' +
                ", isStatic=" + isStatic +
                ", returnType='" + returnType + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                ", parameters=" + parameters.size() +
                '}';
    }

    /**
     * 方法参数
     */
    public static class MethodParameter {
        private final String name;
        private final String type;

        public MethodParameter(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return name + ": " + type;
        }
    }
}
