package org.example.harmony;

import org.example.harmony.model.DependencyInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HarDependencyEngine 测试类
 */
class HarDependencyEngineTest {

    private final HarDependencyEngine engine = new HarDependencyEngine();

    @Test
    void testProcessEmptyModule(@TempDir Path tempDir) throws IOException {
        // 创建空的模块目录结构
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        HarDependencyEngine.EngineResult result = engine.process(tempDir.toString());

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDependencies().size());
    }

    @Test
    void testProcessWithThirdPartyDependency(@TempDir Path tempDir) throws IOException {
        // 创建项目结构：tempDir作为项目根目录，创建一个模块子目录
        Path moduleDir = tempDir.resolve("my-module");
        Path srcMain = moduleDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { PermissionUtil } from '@pura/harmony-utils';

                export class Test {}
                """;

        Files.writeString(srcMain.resolve("Test.ets"), content);

        // 创建oh-package.json5
        String packageJson = """
                {
                  "name": "test-module",
                  "version": "1.0.0",
                  "dependencies": {}
                }
                """;
        Files.writeString(moduleDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(moduleDir.toString());

        assertTrue(result.isSuccess());
        assertEquals(1, result.getDependencies().size());
        assertTrue(result.getDependencies().containsKey("@pura/harmony-utils"));

        // 验证生成的文件（在项目根目录下，即tempDir）
        Path depPath = tempDir.resolve("@pura/harmony-utils");
        assertTrue(Files.exists(depPath));
        assertTrue(Files.exists(depPath.resolve("index.d.ts")));
        assertTrue(Files.exists(depPath.resolve("oh-package.json5")));
    }

    @Test
    void testProcessFiltersSdkDependencies(@TempDir Path tempDir) throws IOException {
        // 创建项目结构
        Path moduleDir = tempDir.resolve("my-module");
        Path srcMain = moduleDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { PermissionUtil } from '@pura/harmony-utils';
                import { Ability } from '@kit.AbilityKit';
                import { hvigor } from '@ohos/hvigor-ohos-plugin';
                import { hms } from '@hms/core';

                export class Test {}
                """;

        Files.writeString(srcMain.resolve("Test.ets"), content);

        // 创建oh-package.json5
        String packageJson = """
                {
                  "name": "test-module",
                  "version": "1.0.0",
                  "dependencies": {}
                }
                """;
        Files.writeString(moduleDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(moduleDir.toString());

        assertTrue(result.isSuccess());

        // 只有第三方依赖应该被处理
        assertEquals(1, result.getDependencies().size());
        assertTrue(result.getDependencies().containsKey("@pura/harmony-utils"));
        assertFalse(result.getDependencies().containsKey("@kit.AbilityKit"));
        assertFalse(result.getDependencies().containsKey("@ohos/hvigor-ohos-plugin"));
        assertFalse(result.getDependencies().containsKey("@hms/core"));

        // 只有第三方依赖应该生成文件（在项目根目录下）
        Path puraModule = tempDir.resolve("@pura/harmony-utils");
        assertTrue(Files.exists(puraModule));

        Path kitModule = tempDir.resolve("kit.AbilityKit");
        assertFalse(Files.exists(kitModule));
    }

    @Test
    void testProcessUpdatesOhPackageJson(@TempDir Path tempDir) throws IOException {
        // 创建项目结构
        Path moduleDir = tempDir.resolve("my-module");
        Path srcMain = moduleDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { PermissionUtil } from '@pura/harmony-utils';
                import * as lodash from 'lodash';

                export class Test {}
                """;

        Files.writeString(srcMain.resolve("Test.ets"), content);

        // 创建oh-package.json5
        String packageJson = """
                {
                  "name": "test-module",
                  "version": "1.0.0",
                  "dependencies": {}
                }
                """;
        Path packagePath = moduleDir.resolve("oh-package.json5");
        Files.writeString(packagePath, packageJson);

        HarDependencyEngine.EngineResult result = engine.process(moduleDir.toString());

        assertTrue(result.isSuccess());

        // 验证oh-package.json5被更新
        String updatedContent = Files.readString(packagePath);
        assertTrue(updatedContent.contains("@pura/harmony-utils"));
        assertTrue(updatedContent.contains("lodash"));
        // 路径应该是 file:../xxx (指向项目根目录)
        assertTrue(updatedContent.contains("file:../"));
    }

    @Test
    void testProcessWithOnlySdkDependencies(@TempDir Path tempDir) throws IOException {
        // 创建项目结构
        Path moduleDir = tempDir.resolve("my-module");
        Path srcMain = moduleDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { Ability } from '@kit.AbilityKit';
                import { hvigor } from '@ohos/hvigor-ohos-plugin';

                export class Test {}
                """;

        Files.writeString(srcMain.resolve("Test.ets"), content);

        // 创建oh-package.json5
        String packageJson = """
                {
                  "name": "test-module",
                  "version": "1.0.0",
                  "dependencies": {}
                }
                """;
        Files.writeString(moduleDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(moduleDir.toString());

        assertTrue(result.isSuccess());
        // 当只有SDK依赖时，返回allDependencies（用于信息展示）
        // 但不会生成文件
        assertTrue(result.getDependencies().size() >= 2);

        // 不应该在项目根目录生成依赖目录
        Path depDir = tempDir.resolve("@kit");
        assertFalse(Files.exists(depDir));
    }

    @Test
    void testEngineResultGetters(@TempDir Path tempDir) throws IOException {
        // 创建项目结构
        Path moduleDir = tempDir.resolve("my-module");
        Path srcMain = moduleDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { Util } from 'test-lib';

                export class Test {}
                """;

        Files.writeString(srcMain.resolve("Test.ets"), content);

        HarDependencyEngine.EngineResult result = engine.process(moduleDir.toString());

        assertTrue(result.isSuccess());
        assertEquals(moduleDir.toString(), result.getModulePath());
        assertEquals(tempDir.toString(), result.getProjectRootPath());
        assertEquals(1, result.getDependencies().size());
    }

    @Test
    void testProcessMultipleThirdPartyDependencies(@TempDir Path tempDir) throws IOException {
        // 创建项目结构
        Path moduleDir = tempDir.resolve("my-module");
        Path srcMain = moduleDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { Util1 } from 'lib1';
                import { Util2 } from 'lib2';
                import { Util3 } from '@scoped/lib3';

                export class Test {}
                """;

        Files.writeString(srcMain.resolve("Test.ets"), content);

        // 创建oh-package.json5
        String packageJson = """
                {
                  "name": "test-module",
                  "version": "1.0.0",
                  "dependencies": {}
                }
                """;
        Files.writeString(moduleDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(moduleDir.toString());

        assertTrue(result.isSuccess());
        assertEquals(3, result.getDependencies().size());

        // 验证所有依赖都已生成（在项目根目录下）
        Path lib1 = tempDir.resolve("lib1");
        Path lib2 = tempDir.resolve("lib2");
        Path lib3 = tempDir.resolve("@scoped/lib3");

        assertTrue(Files.exists(lib1));
        assertTrue(Files.exists(lib2));
        assertTrue(Files.exists(lib3));
    }
}
