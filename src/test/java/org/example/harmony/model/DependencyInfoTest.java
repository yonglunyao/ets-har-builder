package org.example.harmony.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DependencyInfo 测试类
 */
class DependencyInfoTest {

    @Test
    void testCreateDependencyInfo() {
        DependencyInfo depInfo = new DependencyInfo("@pura/harmony-utils");

        assertEquals("@pura/harmony-utils", depInfo.getModulePath());
        assertTrue(depInfo.getImportInfos().isEmpty());
        assertTrue(depInfo.getReferencedTypes().isEmpty());
        assertTrue(depInfo.getExtraDeclarations().isEmpty());
    }

    @Test
    void testAddImportInfo() {
        DependencyInfo depInfo = new DependencyInfo("lodash");
        ImportInfo importInfo = new ImportInfo("lodash", "lodash", null, false);

        depInfo.addImportInfo(importInfo);

        assertEquals(1, depInfo.getImportInfos().size());
        assertEquals("lodash", depInfo.getImportInfos().get(0).getImportedName());
    }

    @Test
    void testAddReferencedType() {
        DependencyInfo depInfo = new DependencyInfo("lodash");
        TypeInfo typeInfo = new TypeInfo("lodash", "namespace", "declare namespace lodash {}");

        depInfo.addReferencedType(typeInfo);

        assertEquals(1, depInfo.getReferencedTypes().size());
        assertEquals("lodash", depInfo.getReferencedTypes().get(0).getName());
    }

    @Test
    void testAddExtraDeclaration() {
        DependencyInfo depInfo = new DependencyInfo("my-lib");
        depInfo.addExtraDeclaration("CustomType", "type CustomType = string;");

        assertEquals(1, depInfo.getExtraDeclarations().size());
        assertEquals("type CustomType = string;", depInfo.getExtraDeclarations().get("CustomType"));
    }

    @Test
    void testGetOhPackageName_WithScopedPackage() {
        DependencyInfo depInfo = new DependencyInfo("@pura/harmony-utils");
        assertEquals("pura_harmony-utils", depInfo.getOhPackageName());
    }

    @Test
    void testGetOhPackageName_WithRegularPackage() {
        DependencyInfo depInfo = new DependencyInfo("lodash");
        assertEquals("lodash", depInfo.getOhPackageName());
    }

    @Test
    void testGetOhPackageName_WithPath() {
        DependencyInfo depInfo = new DependencyInfo("@types/node");
        assertEquals("types_node", depInfo.getOhPackageName());
    }

    @Test
    void testToString() {
        DependencyInfo depInfo = new DependencyInfo("test-lib");
        TypeInfo typeInfo = new TypeInfo("Test", "class", "class Test {}");
        depInfo.addReferencedType(typeInfo);
        ImportInfo importInfo = new ImportInfo("test-lib", "Test", null, false);
        depInfo.addImportInfo(importInfo);

        String str = depInfo.toString();
        assertTrue(str.contains("test-lib"));
        assertTrue(str.contains("1"));  // size values
        assertTrue(str.contains("referencedTypes=1"));
        assertTrue(str.contains("importInfos=1"));
    }
}
