package app.collection.wrap;

import java.util.Iterator;

public class ReadOnlyIteratorWrap<E> implements Iterator<E> {
    private Iterator<E> iterator;

    public ReadOnlyIteratorWrap(Iterator<E> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public E next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
