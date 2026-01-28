package org.example.harmony.scanner;

import org.example.harmony.model.DependencyInfo;
import org.example.harmony.model.ImportInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DependencyScanner 测试类
 */
class DependencyScannerTest {

    private final DependencyScanner scanner = new DependencyScanner();

    @Test
    void testScanEmptyModule(@TempDir Path tempDir) throws IOException {
        // 创建空的模块目录结构
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        Map<String, DependencyInfo> dependencies = scanner.scan(tempDir.toString());

        assertTrue(dependencies.isEmpty());
    }

    @Test
    void testScanWithSingleDependency(@TempDir Path tempDir) throws IOException {
        // 创建测试文件
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { PermissionUtil } from '@pura/harmony-utils';

                export class Test {}
                """;

        Path testFile = srcMain.resolve("Test.ets");
        Files.writeString(testFile, content);

        Map<String, DependencyInfo> dependencies = scanner.scan(tempDir.toString());

        assertEquals(1, dependencies.size());
        assertTrue(dependencies.containsKey("@pura/harmony-utils"));
    }

    @Test
    void testScanWithMultipleDependencies(@TempDir Path tempDir) throws IOException {
        // 创建测试文件
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { PermissionUtil } from '@pura/harmony-utils';
                import * as lodash from 'lodash';
                import { Component } from '@kit.ArkUI';
                import React from 'react';

                export class Test {}
                """;

        Path testFile = srcMain.resolve("Test.ets");
        Files.writeString(testFile, content);

        Map<String, DependencyInfo> dependencies = scanner.scan(tempDir.toString());

        assertEquals(4, dependencies.size());
        assertTrue(dependencies.containsKey("@pura/harmony-utils"));
        assertTrue(dependencies.containsKey("lodash"));
        assertTrue(dependencies.containsKey("@kit.ArkUI"));
        assertTrue(dependencies.containsKey("react"));
    }

    @Test
    void testScanIgnoresRelativeImports(@TempDir Path tempDir) throws IOException {
        // 创建测试文件
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { Utils } from './utils';
                import { Helper } from '../helper';
                import { Config } from './config/Config';

                export class Test {}
                """;

        Path testFile = srcMain.resolve("Test.ets");
        Files.writeString(testFile, content);

        Map<String, DependencyInfo> dependencies = scanner.scan(tempDir.toString());

        // 相对路径导入不应该被识别为外部依赖
        assertTrue(dependencies.isEmpty());
    }

    @Test
    void testScanAggregatesImportsFromMultipleFiles(@TempDir Path tempDir) throws IOException {
        // 创建多个测试文件
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content1 = """
                import { PermissionUtil } from '@pura/harmony-utils';

                export class Test1 {}
                """;

        String content2 = """
                import { checkPermission } from '@pura/harmony-utils';
                import { Button } from '@kit.ArkUI';

                export class Test2 {}
                """;

        Files.writeString(srcMain.resolve("Test1.ets"), content1);
        Files.writeString(srcMain.resolve("Test2.ets"), content2);

        Map<String, DependencyInfo> dependencies = scanner.scan(tempDir.toString());

        assertEquals(2, dependencies.size());
        assertTrue(dependencies.containsKey("@pura/harmony-utils"));
        assertEquals(2, dependencies.get("@pura/harmony-utils").getImportInfos().size());
    }

    @Test
    void testScanRootIndexFile(@TempDir Path tempDir) throws IOException {
        // 创建根目录下的Index.ets
        String content = """
                import { StringUtils } from '@pura/harmony-utils';

                export class StringUtils {}
                """;

        Path indexFile = tempDir.resolve("Index.ets");
        Files.writeString(indexFile, content);

        Map<String, DependencyInfo> dependencies = scanner.scan(tempDir.toString());

        assertEquals(1, dependencies.size());
        assertTrue(dependencies.containsKey("@pura/harmony-utils"));
    }

    @Test
    void testDependencyInfoContainsReferencedTypes(@TempDir Path tempDir) throws IOException {
        // 创建测试文件
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { PermissionUtil, Permissions } from '@pura/harmony-utils';

                export class Test {}
                """;

        Path testFile = srcMain.resolve("Test.ets");
        Files.writeString(testFile, content);

        Map<String, DependencyInfo> dependencies = scanner.scan(tempDir.toString());

        DependencyInfo depInfo = dependencies.get("@pura/harmony-utils");
        assertNotNull(depInfo);
        assertEquals(2, depInfo.getImportInfos().size());

        List<ImportInfo> imports = depInfo.getImportInfos();
        assertTrue(imports.stream().anyMatch(i -> "PermissionUtil".equals(i.getImportedName())));
        assertTrue(imports.stream().anyMatch(i -> "Permissions".equals(i.getImportedName())));
    }
}
