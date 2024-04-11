package app.collection.wrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import app.collection.RCollection;
import app.collection.RMap;
import app.collection.RMapEntry;
import app.collection.RSet;

public class RMapWrapTest {

    @Test
    public void testSize() {
        Map<String, String> map = new HashMap<>();
        RMap<String, String> rMap = new RMapWrap<>(map);

        assertEquals(0, rMap.size());

        map.put("1", "One");
        assertEquals(1, rMap.size());
    }

    @Test
    public void testIsEmpty() {
        Map<String, String> map = new HashMap<>();
        RMap<String, String> rMap = new RMapWrap<>(map);

        assertTrue(rMap.isEmpty());

        map.put("1", "One");
        assertFalse(rMap.isEmpty());
    }

    @Test
    public void testContainsKey() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");

        RMap<String, String> rMap = new RMapWrap<>(map);

        assertTrue(rMap.containsKey("1"));
        assertFalse(rMap.containsKey("2"));
    }

    @Test
    public void testGet() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");

        RMap<String, String> rMap = new RMapWrap<>(map);

        assertEquals("One", rMap.get("1"));
        assertNull(rMap.get("2"));
    }

    @Test
    public void testEntryRSet() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        RMap<String, String> rMap = new RMapWrap<>(map);
        RSet<RMapEntry<String, String>> entrySet = rMap.entryRSet();

        assertEquals(2, entrySet.size());

        Iterator<RMapEntry<String, String>> iterator = entrySet.iterator();
        RMapEntry<String, String> entry = iterator.next();

        // Due to Collectors.toSet implementation using HashSet, the order is not guaranteed
        assertEquals(map.get(entry.getKey()), entry.getValue());

        entry = iterator.next();
        assertEquals(map.get(entry.getKey()), entry.getValue());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testKeyRSet() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        RMap<String, String> rMap = new RMapWrap<>(map);
        RSet<String> keySet = rMap.keyRSet();

        assertEquals(2, keySet.size());

        assertTrue(keySet.contains("1"));
        assertTrue(keySet.contains("2"));
        assertFalse(keySet.contains("3"));
        assertFalse(keySet.contains("One"));
        assertFalse(keySet.contains("Two"));
    }

    @Test
    public void testValues() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "One");
        map.put("2", "Two");

        RMap<String, String> rMap = new RMapWrap<>(map);
        RCollection<String> values = rMap.valuesR();

        assertEquals(2, values.size());

        assertTrue(values.contains("One"));
        assertTrue(values.contains("Two"));
        assertFalse(values.contains("Three"));
        assertFalse(values.contains("1"));
        assertFalse(values.contains("2"));
    }
}
