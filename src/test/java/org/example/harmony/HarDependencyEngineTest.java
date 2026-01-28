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
        // 创建测试模块
        Path srcMain = tempDir.resolve("src/main/ets");
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
        Files.writeString(tempDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(tempDir.toString());

        assertTrue(result.isSuccess());
        assertEquals(1, result.getDependencies().size());
        assertTrue(result.getDependencies().containsKey("@pura/harmony-utils"));

        // 验证生成的文件
        Path ohModules = tempDir.resolve("oh_modules/@pura/harmony-utils");
        assertTrue(Files.exists(ohModules));
        assertTrue(Files.exists(ohModules.resolve("index.d.ts")));
        assertTrue(Files.exists(ohModules.resolve("oh-package.json5")));
    }

    @Test
    void testProcessFiltersSdkDependencies(@TempDir Path tempDir) throws IOException {
        // 创建测试模块
        Path srcMain = tempDir.resolve("src/main/ets");
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
        Files.writeString(tempDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(tempDir.toString());

        assertTrue(result.isSuccess());

        // 只有第三方依赖应该被处理
        assertEquals(1, result.getDependencies().size());
        assertTrue(result.getDependencies().containsKey("@pura/harmony-utils"));
        assertFalse(result.getDependencies().containsKey("@kit.AbilityKit"));
        assertFalse(result.getDependencies().containsKey("@ohos/hvigor-ohos-plugin"));
        assertFalse(result.getDependencies().containsKey("@hms/core"));

        // 只有第三方依赖应该生成文件
        Path puraModule = tempDir.resolve("oh_modules/@pura/harmony-utils");
        assertTrue(Files.exists(puraModule));

        Path kitModule = tempDir.resolve("oh_modules/kit.AbilityKit");
        assertFalse(Files.exists(kitModule));
    }

    @Test
    void testProcessUpdatesOhPackageJson(@TempDir Path tempDir) throws IOException {
        // 创建测试模块
        Path srcMain = tempDir.resolve("src/main/ets");
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
        Path packagePath = tempDir.resolve("oh-package.json5");
        Files.writeString(packagePath, packageJson);

        HarDependencyEngine.EngineResult result = engine.process(tempDir.toString());

        assertTrue(result.isSuccess());

        // 验证oh-package.json5被更新
        String updatedContent = Files.readString(packagePath);
        assertTrue(updatedContent.contains("@pura/harmony-utils"));
        assertTrue(updatedContent.contains("lodash"));
        assertTrue(updatedContent.contains("file:./oh_modules/"));
    }

    @Test
    void testProcessWithOnlySdkDependencies(@TempDir Path tempDir) throws IOException {
        // 创建只有SDK依赖的模块
        Path srcMain = tempDir.resolve("src/main/ets");
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
        Files.writeString(tempDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(tempDir.toString());

        assertTrue(result.isSuccess());
        // 当只有SDK依赖时，返回allDependencies（用于信息展示）
        // 但不会生成文件
        assertTrue(result.getDependencies().size() >= 2);

        // 不应该生成oh_modules目录
        Path ohModules = tempDir.resolve("oh_modules");
        assertFalse(Files.exists(ohModules));
    }

    @Test
    void testEngineResultGetters(@TempDir Path tempDir) throws IOException {
        // 创建测试模块
        Path srcMain = tempDir.resolve("src/main/ets");
        Files.createDirectories(srcMain);

        String content = """
                import { Util } from 'test-lib';

                export class Test {}
                """;

        Files.writeString(srcMain.resolve("Test.ets"), content);

        HarDependencyEngine.EngineResult result = engine.process(tempDir.toString());

        assertTrue(result.isSuccess());
        assertEquals(tempDir.toString(), result.getModulePath());
        assertEquals(Paths.get(tempDir.toString(), "oh_modules").toString(), result.getOhModulesPath());
        assertEquals(1, result.getDependencies().size());
    }

    @Test
    void testProcessMultipleThirdPartyDependencies(@TempDir Path tempDir) throws IOException {
        // 创建测试模块
        Path srcMain = tempDir.resolve("src/main/ets");
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
        Files.writeString(tempDir.resolve("oh-package.json5"), packageJson);

        HarDependencyEngine.EngineResult result = engine.process(tempDir.toString());

        assertTrue(result.isSuccess());
        assertEquals(3, result.getDependencies().size());

        // 验证所有依赖都已生成
        Path lib1 = tempDir.resolve("oh_modules/lib1");
        Path lib2 = tempDir.resolve("oh_modules/lib2");
        Path lib3 = tempDir.resolve("oh_modules/@scoped/lib3");

        assertTrue(Files.exists(lib1));
        assertTrue(Files.exists(lib2));
        assertTrue(Files.exists(lib3));
    }
}
