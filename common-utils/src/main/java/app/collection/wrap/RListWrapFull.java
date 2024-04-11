package app.collection.wrap;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import app.collection.RList;

public class RListWrapFull<E> extends RCollectionWrapFull<E> implements List<E>, RList<E> {
    private List<E> list;

    public RListWrapFull(List<E> list) {
        super(list);
        this.list = list;
    }

    @Override
    public RList<E> subRList(int fromIndexInclusive, int toIndexExclusive) {
        List<E> subList = list.subList(fromIndexInclusive, toIndexExclusive);
        return new RListWrap<>(subList);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("addAll");
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("set");
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("add");
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ReadOnlyListIteratorWrap<>(list.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new ReadOnlyListIteratorWrap<>(list.listIterator(index));
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        List<E> subList = list.subList(fromIndex, toIndex);
        return new RListWrapFull<>(subList);
    }
}
