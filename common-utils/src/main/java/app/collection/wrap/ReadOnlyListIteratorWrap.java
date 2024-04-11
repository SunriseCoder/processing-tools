package app.collection.wrap;

import java.util.ListIterator;

public class ReadOnlyListIteratorWrap<E> implements ListIterator<E> {
    private ListIterator<E> iterator;

    public ReadOnlyListIteratorWrap(ListIterator<E> iterator) {
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
    public boolean hasPrevious() {
        return iterator.hasPrevious();
    }

    @Override
    public E previous() {
        return iterator.previous();
    }

    @Override
    public int nextIndex() {
        return iterator.nextIndex();
    }

    @Override
    public int previousIndex() {
        return iterator.previousIndex();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public void set(E e) {
        throw new UnsupportedOperationException("set");
    }

    @Override
    public void add(E e) {
        throw new UnsupportedOperationException("add");
    }
}
