package app.collection;

public interface RMap<K, V> {
    int size();
    boolean isEmpty();
    boolean containsKey(K key);
    V get(K key);

    RSet<RMapEntry<K, V>> entryRSet();
    RSet<K> keyRSet();
    RCollection<V> valuesR();
}
