package app.collection.wrap;

import java.util.Set;

import app.collection.RSet;

public class RSetWrapFull<E> extends RCollectionWrapFull<E> implements Set<E>, RSet<E> {

    public RSetWrapFull(Set<E> set) {
        super(set);
    }
}
