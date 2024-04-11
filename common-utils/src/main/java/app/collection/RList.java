package app.collection;

public interface RList<E> extends RCollection<E> {
    E get(int index);
    int indexOf(E element);
    int lastIndexOf(E element);
    RList<E> subRList(int fromIndexInclusive, int toIndexExclusive);
}
