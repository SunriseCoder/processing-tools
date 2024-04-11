package app.collection.wrap;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import app.collection.RCollection;

public class RCollectionUtilsTest {

    @Test
    public void testGetSourceCollection() {
        Collection<String> collection = new ArrayList<>();

        RCollection<String> rCollection = new RCollectionWrap<>(collection);
        assertTrue(RCollectionUtils.getSourceCollection(rCollection) == collection);

        rCollection = new RCollectionWrapFull<>(collection);
        assertTrue(RCollectionUtils.getSourceCollection(rCollection) == collection);
    }
}
