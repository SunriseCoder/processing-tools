package app.collection.wrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

public class ReadOnlyIteratorWrapTest {

    @Test
    public void testHasNext() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyIteratorWrap<String> iterator = new ReadOnlyIteratorWrap<>(list.iterator());
        assertTrue(iterator.hasNext());
        assertEquals("Word", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testNext() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyIteratorWrap<String> iterator = new ReadOnlyIteratorWrap<>(list.iterator());
        assertEquals("Word", iterator.next());
        try {
            iterator.next();
            fail("Iterator has no more elements, must throw an exception");
        } catch (NoSuchElementException e) {}
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        List<String> list = new ArrayList<>();
        list.add("Word");

        ReadOnlyIteratorWrap<String> iterator = new ReadOnlyIteratorWrap<>(list.iterator());
        iterator.next();
        iterator.remove();
    }
}
