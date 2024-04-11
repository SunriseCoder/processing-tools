package app.collection.wrap;

import java.util.Map;

public class RMapEntryWrapFull<K, V> extends RMapEntryWrap<K, V> implements Map.Entry<K, V> {

    public RMapEntryWrapFull(Map.Entry<K, V> entry) {
        super(entry);
    }

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException("setValue");
    }
}
