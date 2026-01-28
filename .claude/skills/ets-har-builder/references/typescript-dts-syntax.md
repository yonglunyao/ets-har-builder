# TypeScript DTS 语法支持清单

TypeScript 声明文件（.d.ts）支持以下主要语法。理解这些是开发的基础。

## 完整语法清单

| 语法类别 | 语法特性 | 描述 | 当前支持 |
|---------|---------|------|---------|
| **基础类型** | 基本类型 | string, number, boolean, void, null, undefined, unknown, never, any | ✅ |
| **基础类型** | 数组类型 | T[], Array<T>, ReadonlyArray<T> | ✅ |
| **基础类型** | 元组类型 | [T1, T2, ...] | ❌ |
| **基础类型** | 联合类型 | T1 \| T2 \| T3 | ⚠️ 部分 |
| **基础类型** | 交叉类型 | T1 & T2 & T3 | ❌ |
| **基础类型** | 字面量类型 | "abc", 123, true | ❌ |
| **接口** | 基础接口 | interface Name { ... } | ✅ |
| **接口** | 可选属性 | property?: type | ✅ |
| **接口** | 只读属性 | readonly property: type | ⚠️ 部分 |
| **接口** | 索引签名 | [key: string]: type | ✅ |
| **接口** | 方法签名 | method(args): return | ✅ |
| **接口** | 调用签名 | (args): return | ❌ |
| **接口** | 构造签名 | new (args): return | ❌ |
| **接口** | 泛型接口 | interface Name<T> { ... } | ❌ |
| **接口** | 继承 | interface B extends A { ... } | ❌ |
| **类型别名** | 基础别名 | type Name = type | ❌ |
| **类型别名** | 泛型别名 | type Name<T> = ... | ❌ |
| **类型别名** | 条件类型 | T extends U ? X : Y | ❌ |
| **类型别名** | 映射类型 | { [K in keyof T]: ... } | ❌ |
| **类型别名** | 模板字面量类型 | `${Prefix}${Suffix}` | ❌ |
| **类** | 类声明 | class Name { ... } | ❌ |
| **类** | 泛型类 | class Name<T> { ... } | ❌ |
| **类** | 类继承 | class B extends A { ... } | ❌ |
| **类** | 实现接口 | class A implements B { ... } | ❌ |
| **类** | 静态成员 | static method() {} | ⚠️ 部分 |
| **类** | 抽象类 | abstract class ... | ❌ |
| **枚举** | 数字枚举 | enum Name { A, B } | ❌ |
| **枚举** | 字符串枚举 | enum Name { A = "a", B = "b" } | ❌ |
| **枚举** | 常量枚举 | const enum ... | ❌ |
| **命名空间** | 基础命名空间 | namespace Name { ... } | ✅ |
| **命名空间** | 嵌套命名空间 | namespace A { namespace B { ... } } | ✅ |
| **命名空间** | 命名空间导出 | export const x | ✅ |
| **命名空间** | 命名空间导入 | import ... = ... | ❌ |
| **模块** | ES模块声明 | export { ... }, export default | ✅ |
| **模块** | 模块导出接口 | export interface ... | ✅ |
| **模块** | 模块导出类型 | export type ... | ❌ |
| **模块** | 导入别名 | import { a as b } from ... | ⚠️ 部分 |
| **函数** | 函数声明 | function name(args): return | ✅ |
| **函数** | 函数重载 | function name(args1): return1; function name(args2): return2; | ❌ |
| **函数** | 泛型函数 | function name<T>(arg: T): T | ❌ |
| **函数** | 可选参数 | (arg?: type) | ⚠️ 部分 |
| **函数** | 默认参数 | (arg: type = default) | ❌ |
| **函数** | 剩余参数 | (...args: type[]) | ❌ |
| **泛型** | 泛型类型参数 | <T, U> | ❌ |
| **泛型** | 泛型约束 | <T extends U> | ❌ |
| **泛型** | 泛型默认值 | <T = string> | ❌ |
| **泛型** | 条件类型 | T extends U ? X : Y | ❌ |
| **泛型** | 映射类型 | { [K in keyof T]: ... } | ❌ |
| **高级类型** | typeof 操作符 | typeof variable | ❌ |
| **高级类型** | keyof 操作符 | keyof T | ❌ |
| **高级类型** | infer 类型 | infer T | ❌ |
| **高级类型** | Pick<T, K> | ⚠️ 部分 |
| **高级类型** | Partial<T> | ⚠️ 部分 |
| **高级类型** | Required<T> | ⚠️ 部分 |
| **高级类型** | Readonly<T> | ⚠️ 部分 |
| **声明合并** | 接口合并 | 多个 interface 相同名称 | ❌ |
| **声明合并** | 命名空间与函数/类合并 | ❌ | ❌ |
| **全局声明** | declare global | 全局类型声明 | ✅ |
| **全局声明** | declare module | 模块声明 | ⚠️ 部分 |
| **其他** | import type | import type { ... } from ... | ❌ |
| **其他** | export as namespace | export as namespace NS | ❌ |
| **其他** | 类型断言 | value as type | ⚠️ 部分 |
| **其他** | 非空断言 | value! | ❌ |
| **其他** | 装饰器 | @decorator | ❌ |

## 当前已支持语法

1. **基础类型**：string, number, boolean, void, unknown
2. **数组类型**：T[] 基础形式
3. **接口**：
   - 基础接口定义
   - 可选属性 `property?: type`
   - 索引签名 `[key: string]: type`
   - 方法签名 `method(args): return`
4. **命名空间**：
   - 基础命名空间声明
   - 嵌套命名空间
   - 导出成员
5. **函数**：
   - 基础函数声明
   - 简单类型参数
6. **模块**：
   - ES模块导出 `export { ... }`
   - 默认导出 `export default`
   - 导出接口 `export interface`
7. **全局声明**：`declare global` 格式

## 待支持的高优先级语法

1. **泛型支持**：泛型接口、泛型函数、泛型约束
2. **函数重载**：同一函数多种签名
3. **类型别名**：type 声明、条件类型
4. **类声明**：class 语法、构造签名
5. **枚举**：enum 声明和值
6. **声明合并**：接口合并能力
7. **映射类型**：Pick, Partial, Omit 等工具类型
8. **更复杂的数组/对象类型**：元组、联合类型、交叉类型

## 语法实现优先级建议

基于实际使用场景和复杂度，建议的优先级：

| 优先级 | 语法特性 | 原因 |
|-------|---------|------|
| P0 | 函数重载 | 常见库如 crypto-js 有大量重载 |
| P0 | 泛型基础 | 现代库大量使用泛型 |
| P1 | 类型别名 | type 声明很常见 |
| P1 | 枚举 | 配置型常量常用 |
| P1 | 只读属性 | interface 中 readonly 关键字 |
| P2 | 类声明 | 面向对象库需要 |
| P2 | 条件类型 | 高级类型推断需要 |
| P2 | 映射类型 | 工具类型需要 |
| P3 | 模板字面量类型 | 特殊场景需要 |
| P3 | 装饰器 | 装饰器模式需要 |
