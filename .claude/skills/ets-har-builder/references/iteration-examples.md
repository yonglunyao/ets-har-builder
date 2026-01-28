# 迭代开发示例

本文档提供了具体的迭代开发示例，每个迭代都专注于一种或几种 TypeScript DTS 语法场景。

## 迭代 1：函数重载支持

### 场景
crypto-js 库的 `encrypt` 函数有多种调用方式。

### 问题
```typescript
// 源代码中的多种调用
CryptoJS.AES.encrypt(message, key)              // 无 options
CryptoJS.AES.encrypt(message, key, options)     // 带 options
```

### 方案
1. 在 `InterfaceMethodInfo` 中添加 `overloads` 列表
2. 在 `ArkTSParser` 中识别同一函数的多次调用模式
3. 在 `DtsGenerator` 中生成重载声明

### 验证
```typescript
// 生成的 DTS
namespace AES {
    function encrypt(message: string, key: string): UnknownInterface;
    function encrypt(message: string, key: string, options: UnknownInterface): UnknownInterface;
}
```

---

## 迭代 2：泛型基础支持

### 场景
现代库大量使用泛型，如 `Observable<T>`。

### 问题
```typescript
// 源代码
interface Observable<T> {
    subscribe(callback: (value: T) => void): void;
}
const obs: Observable<string>;
```

### 方案
1. 在 `TypeInfo` 中添加 `typeParameters` 字段
2. 在 `ArkTSParser` 中识别 `<T>` 语法
3. 在 `DtsGenerator` 中生成泛型声明

### 验证
```typescript
// 生成的 DTS
interface Observable<T> {
    subscribe(callback: (value: T) => void): void;
}
```

---

## 迭代 3：类型别名（type）支持

### 场景
库中使用 type 定义复杂类型。

### 问题
```typescript
// 源代码
type Config = {
    readonly: boolean;
    deep?: boolean;
};
```

### 方案
1. 添加 `TypeAliasInfo` 模型
2. 在 `ArkTSParser` 中识别 `type` 关键字
3. 在 `DtsGenerator` 中生成 type 声明

### 验证
```typescript
// 生成的 DTS
type Config = {
    readonly: boolean;
    deep?: boolean;
};
```

---

## 迭代 4：枚举支持

### 场景
配置型常量使用 enum。

### 问题
```typescript
// 源代码
enum Encoding {
    Utf8 = 'utf8',
    Base64 = 'base64',
    Hex = 'hex'
}
```

### 方案
1. 添加 `EnumInfo` 模型
2. 在 `ArkTSParser` 中识别 `enum` 关键字
3. 在 `DtsGenerator` 中生成 enum 声明

### 验证
```typescript
// 生成的 DTS
enum Encoding {
    Utf8 = 'utf8',
    Base64 = 'base64',
    Hex = 'hex'
}
```

---

## 迭代 5：声明合并

### 场景
通过多次定义扩展接口。

### 问题
```typescript
// 源代码
interface Window {
    title: string;
}
interface Window {
    width: number;
}
```

### 方案
1. 在 `DependencyScanner` 中合并同名接口的属性
2. 在 `DtsGenerator` 中生成合并后的接口

### 验证
```typescript
// 生成的 DTS
interface Window {
    title: string;
    width: number;
}
```
