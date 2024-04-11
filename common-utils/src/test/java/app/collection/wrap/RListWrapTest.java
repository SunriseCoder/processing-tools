package app.collection.wrap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import app.collection.RList;

public class RListWrapTest {

    @Test
    public void testGet() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");

        RList<String> rList = new RListWrap<>(list);
        assertEquals("Word", rList.get(0));
        assertEquals("Dot", rList.get(1));
        assertEquals("Letter", rList.get(2));
    }

    @Test
    public void testIndexOf() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");

        RList<String> rList = new RListWrap<>(list);
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

        RList<String> rList = new RListWrap<>(list);
        assertEquals(0, rList.lastIndexOf("Word"));
        assertEquals(3, rList.lastIndexOf("Dot"));
        assertEquals(2, rList.lastIndexOf("Letter"));
        assertEquals(-1, rList.lastIndexOf("NotInTheList"));
    }

    @Test
    public void testSubRList() {
        List<String> list = new ArrayList<>();
        list.add("Word");
        list.add("Dot");
        list.add("Letter");
        list.add("Symbol");

        RList<String> rList = new RListWrap<>(list);
        RList<String> subList = rList.subRList(1, 3);
        assertEquals(2, subList.size());
        assertArrayEquals(new String[] { "Dot", "Letter" }, subList.toArray());
    }
}
