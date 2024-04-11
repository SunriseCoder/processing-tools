package app.collection.wrap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import app.collection.RCollection;
import app.collection.RMap;
import app.collection.RMapEntry;
import app.collection.RSet;

public class RMapWrap<K, V> implements RMap<K, V> {
    protected Map<K, V> map;

    public RMapWrap(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public RSet<RMapEntry<K, V>> entryRSet() {
        Set<RMapEntry<K, V>> entrySet = map.entrySet().stream()
                .map(e -> new RMapEntryWrap<>(e)).collect(Collectors.toSet());
        return new RSetWrap<>(entrySet);
    }

    @Override
    public RSet<K> keyRSet() {
        Set<K> keySet = map.keySet();
        return new RSetWrap<>(keySet);
    }

    @Override
    public RCollection<V> valuesR() {
        Collection<V> values = map.values();
        return new RCollectionWrap<>(values);
    }
}
