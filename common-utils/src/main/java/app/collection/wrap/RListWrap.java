package app.collection.wrap;

import java.util.List;

import app.collection.RList;

public class RListWrap<E> extends RCollectionWrap<E> implements RList<E> {
    private List<E> list;

    public RListWrap(List<E> list) {
        super(list);
        this.list = list;
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public int indexOf(E element) {
        return list.indexOf(element);
    }

    @Override
    public int lastIndexOf(E element) {
        return list.lastIndexOf(element);
    }

    @Override
    public RList<E> subRList(int fromIndexInclusive, int toIndexExclusive) {
        List<E> subList = list.subList(fromIndexInclusive, toIndexExclusive);
        return new RListWrap<>(subList);
    }
}
