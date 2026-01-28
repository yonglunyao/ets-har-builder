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
 * 依赖生成在项目根目录下，而非模块内部
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
     * @param projectRootPath 项目根目录路径
     */
    public void generateStructure(Map<String, DependencyInfo> dependencies, String projectRootPath) throws IOException {
        logger.info("Generating dependency structure in project root: {}", projectRootPath);

        for (DependencyInfo dependency : dependencies.values()) {
            String dependencyPath = buildDependencyPath(projectRootPath, dependency.getModulePath());

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
     * 构建依赖的输出路径（在项目根目录下）
     * 将模块路径转换为文件系统路径
     *
     * 例如:
     * - @pura/harmony-utils -> @pura/harmony-utils
     * - lodash -> lodash
     *
     * @param projectRootPath 项目根目录
     * @param modulePath 模块路径
     * @return 完整的依赖路径
     */
    private String buildDependencyPath(String projectRootPath, String modulePath) {
        Path path = Paths.get(projectRootPath);

        if (modulePath.startsWith("@")) {
            // scoped package: @pura/harmony-utils
            String[] parts = modulePath.split("/", 2);
            if (parts.length == 2) {
                path = path.resolve(parts[0]);  // @pura
                path = path.resolve(parts[1]);  // harmony-utils
            } else {
                path = path.resolve(modulePath);
            }
        } else {
            // regular package: lodash
            path = path.resolve(modulePath);
        }

        return path.toString();
    }

    /**
     * 获取项目根目录路径
     * 用于后续更新oh-package.json5中的依赖引用
     *
     * @param projectRootPath 项目根目录
     * @return 项目根目录路径
     */
    public String getProjectRootPath(String projectRootPath) {
        return projectRootPath;
    }
}
