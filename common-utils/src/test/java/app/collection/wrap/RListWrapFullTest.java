package app.collection.wrap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.Test;

import app.collection.RList;

public class RListWrapFullTest {

    @Test
    public void testSubRList() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");
        list.add("Symbol");

        RList<String> rList = new RListWrapFull<>(list);
        RList<String> subList = rList.subRList(1, 3);
        assertEquals(2, subList.size());
        assertArrayEquals(new String[] { "Dot", "Letter" }, subList.toArray());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAll() {
        List<String> list = new ArrayList<>();
        List<String> rList = new RListWrapFull<>(list);

        List<String> toAdd = new ArrayList<>();
        rList.addAll(0, toAdd);
    }

    @Test
    public void testGet() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");

        RList<String> rList = new RListWrapFull<>(list);
        assertEquals("Word", rList.get(0));
        assertEquals("Dot", rList.get(1));
        assertEquals("Letter", rList.get(2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSet() {
        List<String> list = new ArrayList<>();
        List<String> rList = new RListWrapFull<>(list);
        rList.set(0, "Word");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAdd() {
        List<String> list = new ArrayList<>();
        List<String> rList = new RListWrapFull<>(list);
        rList.add(0, "Word");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        List<String> rList = new RListWrapFull<>(list);
        rList.remove(0);
    }

    @Test
    public void testIndexOf() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");

        List<String> rList = new RListWrapFull<>(list);
        assertEquals(0, rList.indexOf("Word"));
        assertEquals(1, rList.indexOf("Dot"));
        assertEquals(2, rList.indexOf("Letter"));
        assertEquals(-1, rList.indexOf("NotInTheList"));
    }

    @Test
    public void testLastIndexOf() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");
        list.add("Dot");

        List<String> rList = new RListWrapFull<>(list);
        assertEquals(0, rList.lastIndexOf("Word"));
        assertEquals(3, rList.lastIndexOf("Dot"));
        assertEquals(2, rList.lastIndexOf("Letter"));
        assertEquals(-1, rList.lastIndexOf("NotInTheList"));
    }

    @Test
    public void testListIterator() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");

        List<String> rList = new RListWrapFull<>(list);
        ListIterator<String> iterator = rList.listIterator();

        assertFalse(iterator.hasPrevious());
        assertTrue(iterator.hasNext());

        assertEquals("Word", iterator.next());
        assertTrue(iterator.hasPrevious());
        assertTrue(iterator.hasNext());

        assertEquals("Dot", iterator.next());
        assertTrue(iterator.hasPrevious());
        assertFalse(iterator.hasNext());

        assertEquals("Dot", iterator.previous());
        assertTrue(iterator.hasPrevious());
        assertTrue(iterator.hasNext());

        assertEquals("Dot", iterator.next());
        assertTrue(iterator.hasPrevious());
        assertFalse(iterator.hasNext());

        try {
            iterator.remove();
            fail("Operation \"remove\" in Read-Only Iterator must throw an Exception");
        } catch (UnsupportedOperationException e) {}

        try {
            iterator.next();
            fail("Iterator has no more next elements, must throw an Exception");
        } catch (NoSuchElementException e) {}
    }

    @Test
    public void testListIteratorFrom() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");

        List<String> rList = new RListWrapFull<>(list);
        ListIterator<String> iterator = rList.listIterator(1);

        assertTrue(iterator.hasPrevious());
        assertTrue(iterator.hasNext());

        assertEquals("Dot", iterator.next());
        assertTrue(iterator.hasPrevious());
        assertFalse(iterator.hasNext());

        assertEquals("Dot", iterator.previous());
        assertTrue(iterator.hasPrevious());
        assertTrue(iterator.hasNext());

        assertEquals("Dot", iterator.next());
        assertTrue(iterator.hasPrevious());
        assertFalse(iterator.hasNext());

        try {
            iterator.remove();
            fail("Operation \"remove\" in Read-Only Iterator must throw an Exception");
        } catch (UnsupportedOperationException e) {}

        try {
            iterator.next();
            fail("Iterator has no more next elements, must throw an Exception");
        } catch (NoSuchElementException e) {}
    }

    @Test
    public void testSubList() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");
        list.add("Symbol");

        List<String> rList = new RListWrapFull<>(list);
        List<String> subList = rList.subList(1, 3);
        assertEquals(2, subList.size());
        assertArrayEquals(new String[] { "Dot", "Letter" }, subList.toArray());
    }
}
