package org.example.harmony.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ImportInfo 测试类
 */
class ImportInfoTest {

    @Test
    void testCreateImportInfo() {
        ImportInfo importInfo = new ImportInfo("lodash", "lodash", null, false);

        assertEquals("lodash", importInfo.getModulePath());
        assertEquals("lodash", importInfo.getImportedName());
        assertNull(importInfo.getLocalAlias());
        assertFalse(importInfo.isTypeImport());
    }

    @Test
    void testImportInfoWithAlias() {
        ImportInfo importInfo = new ImportInfo("vue", "Vue", "Vue2", false);

        assertEquals("vue", importInfo.getModulePath());
        assertEquals("Vue", importInfo.getImportedName());
        assertEquals("Vue2", importInfo.getLocalAlias());
    }

    @Test
    void testTypeImport() {
        ImportInfo importInfo = new ImportInfo("react", "React", null, true);

        assertTrue(importInfo.isTypeImport());
    }

    @Test
    void testIsExternalDependency_WithAtSymbol() {
        ImportInfo importInfo = new ImportInfo("@pura/harmony-utils", "PermissionUtil", null, false);
        assertTrue(importInfo.isExternalDependency());
    }

    @Test
    void testIsExternalDependency_WithoutAtSymbol() {
        ImportInfo importInfo = new ImportInfo("lodash", "lodash", null, false);
        assertTrue(importInfo.isExternalDependency());
    }

    @Test
    void testIsExternalDependency_RelativePath() {
        ImportInfo importInfo = new ImportInfo("./utils", "utils", null, false);
        assertFalse(importInfo.isExternalDependency());

        ImportInfo importInfo2 = new ImportInfo("../components", "Button", null, false);
        assertFalse(importInfo2.isExternalDependency());
    }

    @Test
    void testEqualsAndHashCode() {
        ImportInfo info1 = new ImportInfo("lodash", "lodash", null, false);
        ImportInfo info2 = new ImportInfo("lodash", "lodash", null, false);
        ImportInfo info3 = new ImportInfo("lodash", "map", null, false);

        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
        assertNotEquals(info1, info3);
    }
}
