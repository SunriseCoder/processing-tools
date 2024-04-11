package app.collection.wrap;

import java.util.Collection;
import java.util.Iterator;

import app.collection.RCollection;

public class RCollectionWrap<E> implements RCollection<E> {
    protected Collection<E> collection;

    public RCollectionWrap(Collection<E> collection) {
        this.collection = collection;
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(E element) {
        return collection.contains(element);
    }

    @Override
    public Iterator<E> iterator() {
        return new ReadOnlyIteratorWrap<>(collection.iterator());
    }

    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return collection.toArray(a);
    }

    @Override
    public boolean containsAllChecked(Collection<E> collection) {
        return this.collection.containsAll(collection);
    }

    @Override
    public boolean containsAllChecked(RCollection<E> collection) {
        Collection<E> sourceCollection = RCollectionUtils.getSourceCollection(collection);
        return this.collection.containsAll(sourceCollection);
    }

    Collection<E> getSourceCollection() {
        return collection;
    }
}
