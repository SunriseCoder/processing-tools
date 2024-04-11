package app.collection.wrap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RMapWrapFull<K, V> extends RMapWrap<K, V> implements Map<K, V> {

    public RMapWrapFull(Map<K, V> map) {
        super(map);
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("put");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("putAll");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    @Override
    public Set<K> keySet() {
        Set<K> keySet = map.keySet();
        return new RSetWrapFull<>(keySet);
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = map.values();
        return new RCollectionWrapFull<>(values);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entrySet = map.entrySet().stream()
                .map(e -> new RMapEntryWrapFull<>(e)).collect(Collectors.toSet());
        return new RSetWrapFull<>(entrySet);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        throw new UnsupportedOperationException("computeIfAbsent");
    }
}
