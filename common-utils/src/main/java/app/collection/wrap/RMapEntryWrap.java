package app.collection.wrap;

import java.util.Map;
import java.util.Map.Entry;

import app.collection.RMapEntry;

public class RMapEntryWrap<K, V> implements RMapEntry<K, V> {
    protected Entry<K, V> entry;

    public RMapEntryWrap(Map.Entry<K, V> entry) {
        this.entry = entry;
    }

    @Override
    public K getKey() {
        return entry.getKey();
    }

    @Override
    public V getValue() {
        return entry.getValue();
    }
}
