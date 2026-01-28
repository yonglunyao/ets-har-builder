package org.example.harmony.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TypeInfo 测试类
 */
class TypeInfoTest {

    @Test
    void testCreateTypeInfo() {
        TypeInfo typeInfo = new TypeInfo("StringUtils", "class", "class StringUtils {}");

        assertEquals("StringUtils", typeInfo.getName());
        assertEquals("class", typeInfo.getKind());
        assertEquals("class StringUtils {}", typeInfo.getSignature());
        assertNull(typeInfo.getTypeParameters());
    }

    @Test
    void testCreateTypeInfoWithGeneric() {
        TypeInfo typeInfo = new TypeInfo("List", "class", "class List<T> {}", "<T>");

        assertEquals("List", typeInfo.getName());
        assertEquals("<T>", typeInfo.getTypeParameters());
    }

    @Test
    void testClassType() {
        TypeInfo typeInfo = new TypeInfo("User", "class", "class User {}");
        assertEquals("class", typeInfo.getKind());
    }

    @Test
    void testInterfaceType() {
        TypeInfo typeInfo = new TypeInfo("Serializable", "interface", "interface Serializable {}");
        assertEquals("interface", typeInfo.getKind());
    }

    @Test
    void testFunctionType() {
        TypeInfo typeInfo = new TypeInfo("formatDate", "function", "function formatDate(date: Date): string");
        assertEquals("function", typeInfo.getKind());
    }

    @Test
    void testEnumType() {
        TypeInfo typeInfo = new TypeInfo("Color", "enum", "enum Color { Red, Green, Blue }");
        assertEquals("enum", typeInfo.getKind());
    }

    @Test
    void testToString() {
        TypeInfo typeInfo = new TypeInfo("MyClass", "class", "class MyClass {}", "<T>");
        String str = typeInfo.toString();

        assertTrue(str.contains("MyClass"));
        assertTrue(str.contains("class"));
        assertTrue(str.contains("<T>"));
    }
}
