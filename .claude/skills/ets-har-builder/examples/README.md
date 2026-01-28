# ets-har-builder 参考案例

本目录包含 ets-har-builder 的参考案例，用于验证通用 DTS 生成引擎生成的 DTS 声明文件的正确性。

## 目录结构

```
examples/
├── README.md                    # 本说明文件
├── reference-case.md            # 详细的参考案例文档
├── quick-start.md               # 快速开始指南
├── sources/                     # 测试源文件
└── dist/                        # 参考输出（正确的 DTS 声明）
```

## 参考案例说明

本案例展示如何使用 ets-har-builder 通用引擎为外部依赖生成 DTS 声明文件。

**重要约束**：引擎是完全通用的，不包含任何特定第三方库（如 crypto-js）的硬编码类型或方法。

### 快速开始

参见 [quick-start.md](quick-start.md) 了解如何使用 ets-har-builder 生成依赖声明。
