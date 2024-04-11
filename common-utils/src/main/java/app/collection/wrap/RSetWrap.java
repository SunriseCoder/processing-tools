package app.collection.wrap;

import java.util.Set;

import app.collection.RSet;

public class RSetWrap<E> extends RCollectionWrap<E> implements RSet<E> {

    public RSetWrap(Set<E> set) {
        super(set);
    }
}
