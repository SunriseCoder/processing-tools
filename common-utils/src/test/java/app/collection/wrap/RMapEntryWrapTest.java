package app.collection.wrap;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import app.collection.RMap;
import app.collection.RMapEntry;
import app.collection.RSet;

public class RMapEntryWrapTest {

    @Test
    public void testGetKeyAndGetValue() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "One");

        RMap<String, String> rMap = new RMapWrap<>(map);
        RSet<RMapEntry<String, String>> entrySet = rMap.entryRSet();
        Iterator<RMapEntry<String, String>> iterator = entrySet.iterator();
        RMapEntry<String, String> entry = iterator.next();

        assertEquals("1", entry.getKey());
        assertEquals("One", entry.getValue());
    }
}
