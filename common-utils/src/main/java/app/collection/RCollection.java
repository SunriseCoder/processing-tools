package app.collection;

import java.util.Collection;

public interface RCollection<E> extends Iterable<E> {
    int size();
    boolean isEmpty();
    boolean contains(E element);
    Object[] toArray();
    <T> T[] toArray(T[] a);
    boolean containsAllChecked(Collection<E> collection);
    boolean containsAllChecked(RCollection<E> collection);
}
