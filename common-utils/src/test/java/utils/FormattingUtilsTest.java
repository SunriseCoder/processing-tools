package utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FormattingUtilsTest {

    @Test
    public void testHumanReadableSize() {
        assertEquals("0", FormattingUtils.humanReadableSize(0L));
        assertEquals("100", FormattingUtils.humanReadableSize(100L));
        assertEquals("4,0k", FormattingUtils.humanReadableSize(4L * 1024));
        assertEquals("4,1k", FormattingUtils.humanReadableSize(4L * 1024 + 105));
        assertEquals("4,0M", FormattingUtils.humanReadableSize(4L * 1024 * 1024));
        assertEquals("40M", FormattingUtils.humanReadableSize(40L * 1024 * 1024));
        assertEquals("4,0G", FormattingUtils.humanReadableSize(4L * 1024 * 1024 * 1024));
        assertEquals("4,0T", FormattingUtils.humanReadableSize(4L * 1024 * 1024 * 1024 * 1024));
    }

    @Test
    public void testHumanReadableTimeS() {
        assertEquals("00:00:00", FormattingUtils.humanReadableTimeS(0));
        assertEquals("00:00:05", FormattingUtils.humanReadableTimeS(5));
        assertEquals("00:01:05", FormattingUtils.humanReadableTimeS(65));
        assertEquals("01:01:05", FormattingUtils.humanReadableTimeS(3665));
        assertEquals("1:05:08:03", FormattingUtils.humanReadableTimeS(104883));
    }

    @Test
    public void testHumanReadableTimeMS() {
        assertEquals("00:00:00.000", FormattingUtils.humanReadableTimeMS(0));
        assertEquals("00:00:00.002", FormattingUtils.humanReadableTimeMS(2));
        assertEquals("00:00:00.025", FormattingUtils.humanReadableTimeMS(25));
        assertEquals("00:00:00.325", FormattingUtils.humanReadableTimeMS(325));
        assertEquals("00:00:05.017", FormattingUtils.humanReadableTimeMS(5017));
        assertEquals("00:01:05.283", FormattingUtils.humanReadableTimeMS(65283));
        assertEquals("01:01:05.128", FormattingUtils.humanReadableTimeMS(3665128));
        assertEquals("1:05:08:03.091", FormattingUtils.humanReadableTimeMS(104883091));
    }
}
