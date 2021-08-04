package backuper.helpers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FormattingHelperTest {

    @Test
    public void testHumanReadableSize() {
        assertEquals("0b", FormattingHelper.humanReadableSize(0L));
        assertEquals("100b", FormattingHelper.humanReadableSize(100L));
        assertEquals("4,0kb", FormattingHelper.humanReadableSize(4L * 1024));
        assertEquals("4,1kb", FormattingHelper.humanReadableSize(4L * 1024 + 105));
        assertEquals("4,0Mb", FormattingHelper.humanReadableSize(4L * 1024 * 1024));
        assertEquals("40Mb", FormattingHelper.humanReadableSize(40L * 1024 * 1024));
        assertEquals("4,0Gb", FormattingHelper.humanReadableSize(4L * 1024 * 1024 * 1024));
        assertEquals("4,0Tb", FormattingHelper.humanReadableSize(4L * 1024 * 1024 * 1024 * 1024));
    }

    @Test
    public void testHumanReadableTime() {
        assertEquals("00:00:00", FormattingHelper.humanReadableTime(0));
        assertEquals("00:00:05", FormattingHelper.humanReadableTime(5));
        assertEquals("00:01:05", FormattingHelper.humanReadableTime(65));
        assertEquals("01:01:05", FormattingHelper.humanReadableTime(3665));
        assertEquals("1:05:08:03", FormattingHelper.humanReadableTime(104883));
    }
}
