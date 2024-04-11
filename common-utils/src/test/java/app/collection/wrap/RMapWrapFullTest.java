package app.collection.wrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

public class RMapWrapFullTest {
    @Test
    public void testContainsKey() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);

        assertTrue(mapWrap.containsKey("1"));
        assertTrue(mapWrap.containsKey("2"));
        assertFalse(mapWrap.containsKey("3"));
    }

    @Test
    public void testContainsValue() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);

        assertTrue(mapWrap.containsValue("One"));
        assertTrue(mapWrap.containsValue("Two"));
        assertFalse(mapWrap.containsValue("Three"));
    }

    @Test
    public void testGet() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);

        assertEquals("One", mapWrap.get("1"));
        assertEquals("Two", mapWrap.get("2"));
        assertNull(mapWrap.get("3"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPut() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> mapWrap = new RMapWrapFull<>(map);

        mapWrap.put("1", "One");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveKey() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);
        mapWrap.remove("1");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveKeyAndValue() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);
        mapWrap.remove("1", "One");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAll() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> mapWrap = new RMapWrapFull<>(map);

        Map<String, String> toPut = new HashMap<>();
        toPut.put("1", "One");

        mapWrap.putAll(toPut);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClear() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> mapWrap = new RMapWrapFull<>(map);

        mapWrap.clear();
    }

    @Test
    public void testKeySet() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);
        Set<String> keySet = mapWrap.keySet();

        assertEquals(2, keySet.size());

        assertTrue(keySet.contains("1"));
        assertTrue(keySet.contains("2"));
        assertFalse(keySet.contains("3"));

        try {
            keySet.remove("1");
            fail("Read Only Collection must NOT let remove operations, must throw an exception");
        } catch (UnsupportedOperationException e) { }
    }

    @Test
    public void testValues() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);
        Collection<String> values = mapWrap.values();

        assertEquals(2, values.size());

        assertTrue(values.contains("One"));
        assertTrue(values.contains("Two"));
        assertFalse(values.contains("Three"));

        try {
            values.remove("One");
            fail("Read Only Collection must NOT let remove operations, must throw an exception");
        } catch (UnsupportedOperationException e) { }
    }

    @Test
    public void testEntrySet() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        Map<String, String> mapWrap = new RMapWrapFull<>(map);
        Set<Entry<String, String>> entrySet = mapWrap.entrySet();

        assertEquals(2, entrySet.size());

        Iterator<Entry<String, String>> iterator = entrySet.iterator();
        Entry<String, String> entry = iterator.next();

        // Due to Collectors.toSet implementation using HashSet, the order is not guaranteed
        assertEquals(map.get(entry.getKey()), entry.getValue());

        entry = iterator.next();
        assertEquals(map.get(entry.getKey()), entry.getValue());

        assertFalse(iterator.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMapEntryWriteProtection() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        Set<Entry<String, String>> entrySet = mapWrap.entrySet();
        Entry<String, String> entry = entrySet.iterator().next();
        entry.setValue("Two");

        assertEquals("One", entry.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReplaceByKey() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.replace("1", "Two");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReplaceByKeyAndOldValue() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.replace("1", "One", "Two");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReplaceAll() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.replaceAll((k, v) -> "New");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutIfAbsent() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.putIfAbsent("2", "Two");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCompute() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.compute("1", (k, v) -> "Value");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testComputeIfAbsent() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.computeIfAbsent("1", k -> "Two");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testComputeIfPresent() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.computeIfPresent("1", (k, v) -> "Two");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMerge() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("1", "One");

        Map<String, String> mapWrap = new RMapWrapFull<>(sourceMap);
        mapWrap.merge("2", "Two", (k, v) -> v);
    }
}
