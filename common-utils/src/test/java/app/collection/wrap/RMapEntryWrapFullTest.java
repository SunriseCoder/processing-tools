package app.collection.wrap;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

public class RMapEntryWrapFullTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testSetValue() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "One");

        Entry<String, String> entry = map.entrySet().iterator().next();
        Map.Entry<String, String> entryWrap = new RMapEntryWrapFull<>(entry);

        // For ReadOnly Entry the operation is not supported, so this should throw an exception
        entryWrap.setValue("Two");

        // Or at least not change the value
        assertEquals("One", entryWrap.getValue());
    }
}
