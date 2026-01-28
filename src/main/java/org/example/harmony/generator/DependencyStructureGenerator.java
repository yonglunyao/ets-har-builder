package org.example.harmony.generator;

import org.example.harmony.model.DependencyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 依赖目录结构生成器
 * 负责为每个外部依赖创建相应的目录结构
 */
public class DependencyStructureGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DependencyStructureGenerator.class);

    private final DtsGenerator dtsGenerator;

    public DependencyStructureGenerator() {
        this.dtsGenerator = new DtsGenerator();
    }

    /**
     * 生成所有依赖的目录结构
     *
     * @param dependencies 依赖信息映射
     * @param baseOutputPath 基础输出路径（通常是模块的oh_modules目录）
     */
    public void generateStructure(Map<String, DependencyInfo> dependencies, String baseOutputPath) throws IOException {
        logger.info("Generating dependency structure in: {}", baseOutputPath);

        for (DependencyInfo dependency : dependencies.values()) {
            String dependencyPath = buildDependencyPath(baseOutputPath, dependency.getModulePath());

            logger.info("  Creating dependency: {} -> {}", dependency.getModulePath(), dependencyPath);

            // 创建目录
            Files.createDirectories(Paths.get(dependencyPath));

            // 生成index.d.ts
            dtsGenerator.generate(dependency, dependencyPath);

            // 生成oh-package.json5
            dtsGenerator.generateOhPackageJson(dependency, dependencyPath);
        }

        logger.info("Generated {} dependencies", dependencies.size());
    }

    /**
     * 构建依赖的输出路径
     * 将模块路径转换为文件系统路径
     *
     * 例如:
     * - @pura/harmony-utils -> oh_modules/@pura/harmony-utils
     * - lodash -> oh_modules/lodash
     */
    private String buildDependencyPath(String basePath, String modulePath) {
        Path path = Paths.get(basePath, "oh_modules");

        if (modulePath.startsWith("@")) {
            // scoped package: @pura/harmony-utils
            String[] parts = modulePath.split("/", 2);
            if (parts.length == 2) {
                path = path.resolve(parts[0]);  // @pura
                path = path.resolve(parts[1]);  // harmony-utils
            } else {
                path = path.resolve(modulePath.substring(1));  // 移除@符号
            }
        } else {
            // regular package: lodash
            path = path.resolve(modulePath);
        }

        return path.toString();
    }

    /**
     * 获取生成的依赖根目录路径
     * 用于后续更新oh-package.json5中的依赖引用
     */
    public String getOhModulesPath(String baseOutputPath) {
        return Paths.get(baseOutputPath, "oh_modules").toString();
    }
}
