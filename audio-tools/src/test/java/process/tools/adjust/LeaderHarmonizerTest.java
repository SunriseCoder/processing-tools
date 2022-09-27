package process.tools.adjust;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LeaderHarmonizerTest {

    @Test
    public void testHasEnoughProximityGroup1BeforeGroup2BeyondProximityNoIntersection() {
        FrameGroup group1 = new FrameGroup(0, 10);
        FrameGroup group2 = new FrameGroup(20, 30);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertFalse(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1BeforeGroup2TouchingProximityOnBorderWithIntersection() {
        FrameGroup group1 = new FrameGroup(0, 10);
        FrameGroup group2 = new FrameGroup(20, 30);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 10);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1BeforeGroup2WithinProximityWithIntersection() {
        FrameGroup group1 = new FrameGroup(0, 10);
        FrameGroup group2 = new FrameGroup(15, 30);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 10);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1BeforeGroup2WithIntersectionTounchingOnlyEnds() {
        FrameGroup group1 = new FrameGroup(0, 10);
        FrameGroup group2 = new FrameGroup(10, 20);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1BeforeGroup2WithIntersection() {
        FrameGroup group1 = new FrameGroup(0, 10);
        FrameGroup group2 = new FrameGroup(5, 15);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1EqualsToGroup2WithIntersection() {
        FrameGroup group1 = new FrameGroup(0, 10);
        FrameGroup group2 = new FrameGroup(0, 10);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1ContainsGroup2WithIntersection() {
        FrameGroup group1 = new FrameGroup(0, 10);
        FrameGroup group2 = new FrameGroup(2, 8);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup2ContainsGroup1WithIntersection() {
        FrameGroup group1 = new FrameGroup(2, 8);
        FrameGroup group2 = new FrameGroup(0, 10);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1AfterGroup2WithIntersection() {
        FrameGroup group1 = new FrameGroup(5, 15);
        FrameGroup group2 = new FrameGroup(0, 10);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1AfterGroup2WithIntersectionTounchingOnlyEnds() {
        FrameGroup group1 = new FrameGroup(10, 20);
        FrameGroup group2 = new FrameGroup(0, 10);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1AfterGroup2WithinProximityWithIntersection() {
        FrameGroup group1 = new FrameGroup(15, 30);
        FrameGroup group2 = new FrameGroup(0, 10);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 10);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1AfterGroup2TouchingProximityOnBorderWithIntersection() {
        FrameGroup group1 = new FrameGroup(20, 30);
        FrameGroup group2 = new FrameGroup(0, 10);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 10);

        assertTrue(actual);
    }

    @Test
    public void testHasEnoughProximityGroup1AfterGroup2BeyondProximityNoIntersection() {
        FrameGroup group1 = new FrameGroup(20, 30);
        FrameGroup group2 = new FrameGroup(0, 10);

        LeaderHarmonizer harmonizer = new LeaderHarmonizer();
        boolean actual = harmonizer.hasEnoughProximity(group1, group2, 5);

        assertFalse(actual);
    }
}
