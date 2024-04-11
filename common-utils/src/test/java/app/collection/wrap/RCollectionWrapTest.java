package app.collection.wrap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import app.collection.RCollection;

public class RCollectionWrapTest {

    @Test
    public void testSize() {
        Collection<String> collection = new ArrayList<>();
        RCollection<String> rCollection = new RCollectionWrap<>(collection);

        assertEquals(0, rCollection.size());

        collection.add("Word");
        assertEquals(1, rCollection.size());
    }

    @Test
    public void testIsEmpty() {
        Collection<String> collection = new ArrayList<>();
        RCollection<String> rCollection = new RCollectionWrap<>(collection);
        assertTrue(rCollection.isEmpty());

        collection.add("Word");
        assertFalse(rCollection.isEmpty());
    }

    @Test
    public void testContains() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        RCollection<String> rCollection = new RCollectionWrap<>(collection);

        assertTrue(rCollection.contains("Word"));
        assertFalse(rCollection.contains("Sentence"));
    }

    @Test
    public void testIterator() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        RCollection<String> rCollection = new RCollectionWrap<>(collection);
        Iterator<String> iterator = rCollection.iterator();

        assertTrue(iterator.hasNext());
        assertEquals("Word", iterator.next());
        assertFalse(iterator.hasNext());
        try {
            iterator.remove();
            fail("Method \"remove()\" must throw an Exception");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void testToArrayNew() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        RCollection<String> rCollection = new RCollectionWrap<>(collection);
        Object[] array = rCollection.toArray();
        assertArrayEquals(new String[] { "Word" }, array);
    }

    @Test
    public void testToArrayExisting() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");

        RCollection<String> rCollection = new RCollectionWrap<>(collection);
        Object[] array1 = new Object[2];
        Object[] array2 = rCollection.toArray(array1);
        assertTrue(array1 == array2);
        assertArrayEquals(new String[] { "Word", null }, array1);
    }

    @Test
    public void testContainsAllCheckedCollection() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");
        collection.add("Letter");
        collection.add("Sentence");

        RCollection<String> rCollection = new RCollectionWrap<>(collection);

        Collection<String> toFind = new ArrayList<>();
        toFind.add("Word");
        toFind.add("Letter");

        assertTrue(rCollection.containsAllChecked(toFind));

        toFind.add("Dot");
        assertFalse(rCollection.containsAllChecked(toFind));
    }

    @Test
    public void testContainsAllCheckedRCollection() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");
        collection.add("Letter");
        collection.add("Sentence");

        RCollection<String> rCollection = new RCollectionWrap<>(collection);

        Collection<String> toFind = new ArrayList<>();
        RCollection<String> toFindRCollection = new RCollectionWrap<>(toFind);
        toFind.add("Word");
        toFind.add("Letter");

        assertTrue(rCollection.containsAllChecked(toFindRCollection));

        toFind.add("Dot");
        assertFalse(rCollection.containsAllChecked(toFindRCollection));
    }

    @Test
    public void testForEachLoop() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Word");
        collection.add("Letter");
        collection.add("Sentence");

        RCollection<String> rCollection = new RCollectionWrap<>(collection);

        List<String> result = new ArrayList<>();
        for (String element : rCollection) {
            result.add(element);
        }

        assertEquals(3, result.size());
        assertArrayEquals(collection.toArray(), result.toArray());
    }
}
