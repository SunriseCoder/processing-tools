package app.collection.wrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

public class ReadOnlyListIteratorWrapTest {

    @Test
    public void testHasNext() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        assertTrue(iterator.hasNext());
        assertEquals("Word", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testNext() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        assertEquals("Word", iterator.next());
        try {
            iterator.next();
            fail("Iterator has no more next elements, must throw an exception");
        } catch (NoSuchElementException e) {}
    }

    @Test
    public void testHasPrevious() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        assertFalse(iterator.hasPrevious());
        assertEquals("Word", iterator.next());
        assertTrue(iterator.hasPrevious());
    }

    @Test
    public void testPrevious() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Digit");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());

        assertEquals("Word", iterator.next());
        assertEquals("Dot", iterator.next());
        assertEquals("Dot", iterator.previous());
        assertEquals("Word", iterator.previous());
        assertEquals("Word", iterator.next());
        assertEquals("Word", iterator.previous());

        try {
            iterator.previous();
            fail("Iterator has no more previous elements, must throw an exception");
        } catch (NoSuchElementException e) {}
    }

    @Test
    public void testNextIndex() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        assertEquals(0, iterator.nextIndex());
        assertEquals(0, iterator.nextIndex());
        iterator.next();
        assertEquals(1, iterator.nextIndex());
        assertEquals(1, iterator.nextIndex());
    }

    @Test
    public void testPreviousIndex() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        assertEquals(-1, iterator.previousIndex());
        assertEquals(-1, iterator.previousIndex());
        iterator.next();
        assertEquals(0, iterator.previousIndex());
        assertEquals(0, iterator.previousIndex());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        iterator.next();
        iterator.remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSet() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        iterator.next();
        iterator.set("Dot");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAdd() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyListIteratorWrap<String> iterator = new ReadOnlyListIteratorWrap<>(list.listIterator());
        iterator.next();
        iterator.add("Dot");
    }
}
