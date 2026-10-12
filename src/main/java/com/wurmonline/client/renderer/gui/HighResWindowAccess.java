package com.wurmonline.client.renderer.gui;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/** Private client fields are resolved once; model and component identities stay native. */
final class HighResWindowAccess {
    private static final Map<String, Field> FIELDS = new HashMap<>();
    private HighResWindowAccess() { }
    static Object get(Object target, String name) {
        try { return field(target, name).get(target); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(name, failure); }
    }
    static void set(Object target, String name, Object value) {
        try { field(target, name).set(target, value); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(name, failure); }
    }
    private static Field field(Object target, String name) throws NoSuchFieldException {
        String key = target.getClass().getName() + "." + name;
        Field found = FIELDS.get(key);
        if (found != null) return found;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                found = type.getDeclaredField(name); found.setAccessible(true); FIELDS.put(key, found); return found;
            } catch (NoSuchFieldException absent) { /* Search the pinned client's base class. */ }
        }
        throw new NoSuchFieldException(key);
    }
}
