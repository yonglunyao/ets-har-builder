package org.example.harmony;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.harmony.generator.DependencyStructureGenerator;
import org.example.harmony.model.DependencyInfo;
import org.example.harmony.scanner.DependencyScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * HarmonyOS HAR依赖生成引擎
 *
 * 功能说明：
 * 扫描HAR模块中的所有源文件，识别外部依赖引用，
 * 为每个外部依赖生成stub声明文件（index.d.ts），
 * 使模块能够在没有实际依赖实现的情况下通过hvigor编译。
 *
 * 使用场景：
 * - 在封闭环境中编译HAR模块
 * - 依赖的第三方组件无法获取，但已知其API签名
 * - 快速搭建测试环境
 *
 * @author ets-har-builder
 * @version 1.0.0
 */
public class HarDependencyEngine {
    private static final Logger logger = LoggerFactory.getLogger(HarDependencyEngine.class);

    /**
     * HarmonyOS SDK依赖前缀（无需生成stub）
     */
    private static final String[] SDK_PREFIXES = {"@kit", "@ohos", "@hms"};

    private final DependencyScanner scanner;
    private final DependencyStructureGenerator structureGenerator;

    public HarDependencyEngine() {
        this.scanner = new DependencyScanner();
        this.structureGenerator = new DependencyStructureGenerator();
    }

    /**
     * 判断是否为HarmonyOS SDK依赖
     */
    private boolean isSdkDependency(String modulePath) {
        for (String prefix : SDK_PREFIXES) {
            if (modulePath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理HAR模块，生成所有外部依赖的stub声明
     * 依赖生成在项目根目录下，而非模块内部
     *
     * @param modulePath HAR模块的根目录路径
     * @return 处理结果，包含生成的依赖路径
     * @throws IOException 如果发生IO错误
     */
    public EngineResult process(String modulePath) throws IOException {
        logger.info("=".repeat(60));
        logger.info("HarDependencyEngine - Starting");
        logger.info("Module path: {}", modulePath);
        logger.info("=".repeat(60));

        // 计算项目根目录（模块的父目录）
        Path moduleDir = Paths.get(modulePath);
        Path projectRootPath = moduleDir.getParent();
        if (projectRootPath == null) {
            projectRootPath = moduleDir.toAbsolutePath().getParent();
        }
        String projectRoot = projectRootPath.toString();

        logger.info("Project root: {}", projectRoot);

        // 1. 扫描模块，收集外部依赖
        Map<String, DependencyInfo> allDependencies = scanner.scan(modulePath);

        if (allDependencies.isEmpty()) {
            logger.info("No external dependencies found, nothing to do.");
            return new EngineResult(modulePath, projectRoot, Map.of(), true);
        }

        // 2. 过滤掉HarmonyOS SDK依赖（@kit, @ohos, @hms）
        Map<String, DependencyInfo> dependencies = new HashMap<>();
        for (DependencyInfo dep : allDependencies.values()) {
            if (isSdkDependency(dep.getModulePath())) {
                logger.info("Skipping SDK dependency: {}", dep.getModulePath());
            } else {
                dependencies.put(dep.getModulePath(), dep);
            }
        }

        if (dependencies.isEmpty()) {
            logger.info("No third-party dependencies found (only SDK dependencies were detected), nothing to do.");
            return new EngineResult(modulePath, projectRoot, allDependencies, true);
        }

        // 3. 生成依赖目录结构（在项目根目录下）
        structureGenerator.generateStructure(dependencies, projectRoot);

        // 4. 更新模块的oh-package.json5，添加本地依赖引用
        updateModuleOhPackage(modulePath, dependencies);

        logger.info("=".repeat(60));
        logger.info("HarDependencyEngine - Completed");
        logger.info("Generated {} dependencies in: {}", dependencies.size(), projectRoot);
        logger.info("The module is now ready for hvigor build.");
        logger.info("=".repeat(60));

        return new EngineResult(modulePath, projectRoot, dependencies, true);
    }

    /**
     * 更新模块的oh-package.json5，添加本地依赖引用
     * 依赖路径相对于模块位置，指向项目根目录
     */
    private void updateModuleOhPackage(String modulePath, Map<String, DependencyInfo> dependencies) throws IOException {
        Path packagePath = Paths.get(modulePath, "oh-package.json5");

        if (!Files.exists(packagePath)) {
            logger.warn("oh-package.json5 not found at: {}", packagePath);
            return;
        }

        String content = Files.readString(packagePath);
        JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();

        // 获取或创建dependencies对象
        JsonObject depsObject;
        if (jsonObject.has("dependencies") && jsonObject.get("dependencies").isJsonObject()) {
            depsObject = jsonObject.getAsJsonObject("dependencies");
        } else {
            depsObject = new JsonObject();
            jsonObject.add("dependencies", depsObject);
        }

        // 添加本地依赖引用（路径指向项目根目录）
        for (DependencyInfo dep : dependencies.values()) {
            String version = depsObject.has(dep.getModulePath())
                    ? depsObject.get(dep.getModulePath()).getAsString()
                    : "file:../" + getRelativeDepPath(dep.getModulePath());

            depsObject.addProperty(dep.getModulePath(), version);
            logger.debug("Added dependency: {} -> {}", dep.getModulePath(), version);
        }

        // 写回文件（保持原有格式）
        Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
        String newContent = gson.toJson(jsonObject);
        Files.writeString(packagePath, newContent, StandardOpenOption.TRUNCATE_EXISTING);

        logger.info("Updated oh-package.json5 with {} local dependencies", dependencies.size());
    }

    /**
     * 获取相对路径
     */
    private String getRelativeDepPath(String modulePath) {
        if (modulePath.startsWith("@")) {
            String[] parts = modulePath.split("/", 2);
            if (parts.length == 2) {
                return parts[0] + "/" + parts[1];
            }
        }
        return modulePath;
    }

    /**
     * 引擎处理结果
     */
    public static class EngineResult {
        private final String modulePath;
        private final String projectRootPath;
        private final Map<String, DependencyInfo> dependencies;
        private final boolean success;

        public EngineResult(String modulePath, String projectRootPath, Map<String, DependencyInfo> dependencies, boolean success) {
            this.modulePath = modulePath;
            this.projectRootPath = projectRootPath;
            this.dependencies = dependencies;
            this.success = success;
        }

        public String getModulePath() {
            return modulePath;
        }

        public String getProjectRootPath() {
            return projectRootPath;
        }

        public Map<String, DependencyInfo> getDependencies() {
            return dependencies;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public String toString() {
            return "EngineResult{" +
                    "modulePath='" + modulePath + '\'' +
                    ", projectRootPath='" + projectRootPath + '\'' +
                    ", dependencies=" + dependencies.size() +
                    ", success=" + success +
                    '}';
        }
    }

    /**
     * 命令行入口
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar ets-har-builder.jar <module-path>");
            System.err.println("Example: java -jar ets-har-builder.jar D:\\code\\secueity\\abc_har\\myutils");
            System.exit(1);
        }

        String modulePath = args[0];

        HarDependencyEngine engine = new HarDependencyEngine();

        try {
            EngineResult result = engine.process(modulePath);

            if (result.isSuccess()) {
                System.out.println("\n✓ SUCCESS");
                System.out.println("  Module: " + result.getModulePath());
                System.out.println("  Dependencies generated: " + result.getDependencies().size());
                System.out.println("  Output path: " + result.getProjectRootPath());
                System.out.println("\nThe module is now ready for hvigor build.");
            } else {
                System.err.println("\n✗ FAILED");
                System.exit(1);
            }

        } catch (Exception e) {
            logger.error("Failed to process module", e);
            System.err.println("\n✗ ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
}
