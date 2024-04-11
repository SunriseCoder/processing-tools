package app.collection.wrap;

import java.util.Collection;

import app.collection.RCollection;
import app.exception.UnsupportedClassException;

public class RCollectionUtils {

    static <E> Collection<E> getSourceCollection(RCollection<E> collection) {
        if (collection instanceof RCollectionWrap) {
            return ((RCollectionWrap<E>) collection).getSourceCollection();
        } else if (collection instanceof RCollectionWrapFull) {
            return ((RCollectionWrapFull<E>) collection).getSourceCollection();
        }

        throw new UnsupportedClassException("Unsupported collection type: " + collection.getClass());
    }
}
