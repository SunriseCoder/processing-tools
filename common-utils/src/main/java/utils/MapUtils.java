package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapUtils {

    public static <K, V> void addToListValue(Map<K, List<V>> map, K key, V value) {
        List<V> list = map.get(key);

        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }

        list.add(value);
    }
}
