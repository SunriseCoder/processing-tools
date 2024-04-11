package app.collection.wrap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

public class RCollectionWrapFullTest {

    @Test
    public void testContains() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);

        assertTrue(rCollection.contains("Word"));
        assertFalse(rCollection.contains("Sentence"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAdd() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);
        rCollection.add("String");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);
        rCollection.remove("Word");
    }

    @Test
    public void testContainsAll() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");
        collection.add("Letter");
        collection.add("Sentence");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);

        Collection<String> toFind = new ArrayList<>();
        toFind.add("Word");
        toFind.add("Letter");

        assertTrue(rCollection.containsAll(toFind));

        toFind.add("Dot");
        assertFalse(rCollection.containsAll(toFind));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAll() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);
        Collection<String> toAdd = new ArrayList<>();
        toAdd.add("Letter");
        rCollection.addAll(toAdd);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAll() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);
        Collection<String> toRemove = new ArrayList<>();
        toRemove.add("Word");
        rCollection.removeAll(toRemove);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveIf() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);
        rCollection.removeIf(e -> true);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAll() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);
        Collection<String> toRetain = new ArrayList<>();
        toRetain.add("Word");
        rCollection.retainAll(toRetain);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClear() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        Collection<String> rCollection = new RCollectionWrapFull<>(collection);
        rCollection.clear();
    }
}
