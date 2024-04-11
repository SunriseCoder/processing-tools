package app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.junit.Test;

public class FormattingUtilsTest {

    @Test
    public void testHumanReadableSizeBi() {
        Locale defaultLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);

        assertEquals("0", FormattingUtils.humanReadableSizeBi(0L));
        assertEquals("100", FormattingUtils.humanReadableSizeBi(100L));
        assertEquals("4.0ki", FormattingUtils.humanReadableSizeBi(4L * 1024));
        assertEquals("4.1ki", FormattingUtils.humanReadableSizeBi(4L * 1024 + 105));
        assertEquals("4.0Mi", FormattingUtils.humanReadableSizeBi(4L * 1024 * 1024));
        assertEquals("40Mi", FormattingUtils.humanReadableSizeBi(40L * 1024 * 1024));
        assertEquals("4.0Gi", FormattingUtils.humanReadableSizeBi(4L * 1024 * 1024 * 1024));
        assertEquals("4.0Ti", FormattingUtils.humanReadableSizeBi(4L * 1024 * 1024 * 1024 * 1024));

        Locale.setDefault(Locale.Category.FORMAT, defaultLocale);
    }

    @Test
    public void testHumanReadableSizeSi() {
        Locale defaultLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);

        assertEquals("0", FormattingUtils.humanReadableSizeSi(0L));
        assertEquals("100", FormattingUtils.humanReadableSizeSi(100L));
        assertEquals("4.0k", FormattingUtils.humanReadableSizeSi(4L * 1000));
        assertEquals("4.1k", FormattingUtils.humanReadableSizeSi(4L * 1000 + 105));
        assertEquals("4.0M", FormattingUtils.humanReadableSizeSi(4L * 1000 * 1000));
        assertEquals("40M", FormattingUtils.humanReadableSizeSi(40L * 1000 * 1000));
        assertEquals("4.0G", FormattingUtils.humanReadableSizeSi(4L * 1000 * 1000 * 1000));
        assertEquals("4.0T", FormattingUtils.humanReadableSizeSi(4L * 1000 * 1000 * 1000 * 1000));

        Locale.setDefault(Locale.Category.FORMAT, defaultLocale);
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

    @Test
    public void testPercentage() {
        // Integers
        assertEquals("0%", FormattingUtils.percentage(0, 100, 0));
        assertEquals("0.0%", FormattingUtils.percentage(0, 100, 1));
        assertEquals("0.00%", FormattingUtils.percentage(0, 100, 2));
        assertEquals("50%", FormattingUtils.percentage(50, 100, 0));
        assertEquals("50.00%", FormattingUtils.percentage(50, 100, 2));
        assertEquals("100%", FormattingUtils.percentage(100, 100, 0));
        assertEquals("100.00%", FormattingUtils.percentage(100, 100, 2));

        // Floats
        assertEquals("9%", FormattingUtils.percentage(3, 35, 0));
        assertEquals("8.57%", FormattingUtils.percentage(3, 35, 2));
    }

    @Test
    public void testAlignLongStringsByRightSide() {
        // Exception on wrong Arguments Number
        try {
            FormattingUtils.alignLongStringsByRightSide(2, "");
            fail("Must throw an exception on wrong arguments number");
        } catch (IllegalArgumentException e) {}

        // 1 Column
        assertEquals("\n", FormattingUtils.alignLongStringsByRightSide(1, ""));
        assertEquals("1\n", FormattingUtils.alignLongStringsByRightSide(1, "1"));
        assertEquals("1\n2\n", FormattingUtils.alignLongStringsByRightSide(1, "1", "2"));
        assertEquals("11\n 2\n", FormattingUtils.alignLongStringsByRightSide(1, "11", "2"));
        assertEquals(" 1\n22\n", FormattingUtils.alignLongStringsByRightSide(1, "1", "22"));
        assertEquals("11\n22\n", FormattingUtils.alignLongStringsByRightSide(1, "11", "22"));

        // 2 Columns 1 Line
        assertEquals(" \n", FormattingUtils.alignLongStringsByRightSide(2, "", ""));
        assertEquals("prefix suffix\n", FormattingUtils.alignLongStringsByRightSide(2, "prefix", "suffix"));

        // 2 Columns 2 Lines
        assertEquals("p1 s1\np2 s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p2", "s2"));
        assertEquals("p1  s1\np2 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p2", "s22"));
        assertEquals(" p1 s1\np22 s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p22", "s2"));
        assertEquals(" p1  s1\np22 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p22", "s22"));
        assertEquals("p1 s11\np2  s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s11", "p2", "s2"));
        assertEquals("p1 s11\np2 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s11", "p2", "s22"));
        assertEquals(" p1 s11\np22  s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s11", "p22", "s2"));
        assertEquals(" p1 s11\np22 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s11", "p22", "s22"));
        assertEquals("p11 s1\n p2 s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s1", "p2", "s2"));
        assertEquals("p11  s1\n p2 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s1", "p2", "s22"));
        assertEquals("p11 s1\np22 s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s1", "p22", "s2"));
        assertEquals("p11  s1\np22 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s1", "p22", "s22"));
        assertEquals("p11 s11\n p2  s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s11", "p2", "s2"));
        assertEquals("p11 s11\n p2 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s11", "p2", "s22"));
        assertEquals("p11 s11\np22  s2\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s11", "p22", "s2"));
        assertEquals("p11 s11\np22 s22\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s11", "p22", "s22"));

        // 3 Columns 1 Line
        assertEquals("  \n", FormattingUtils.alignLongStringsByRightSide(3, "", "", ""));
        assertEquals("prefix middle suffix\n", FormattingUtils.alignLongStringsByRightSide(3, "prefix", "middle", "suffix"));

        // 3 Columns 2 Lines
        assertEquals("p1 m1 s1\np2 m2 s2\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m2", "s2"));
        assertEquals("p11 m1 s1\n p2 m2 s2\n", FormattingUtils.alignLongStringsByRightSide(3, "p11", "m1", "s1", "p2", "m2", "s2"));
        assertEquals("p1 m11 s1\np2  m2 s2\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m11", "s1", "p2", "m2", "s2"));
        assertEquals("p1 m1 s11\np2 m2  s2\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s11", "p2", "m2", "s2"));
        assertEquals(" p1 m1 s1\np22 m2 s2\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p22", "m2", "s2"));
        assertEquals("p1  m1 s1\np2 m22 s2\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m22", "s2"));
        assertEquals("p1 m1  s1\np2 m2 s22\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m2", "s22"));

        // 2 Columns 3 Lines
        assertEquals("p1 s1\np2 s2\np3 s3\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p2", "s2", "p3", "s3"));
        assertEquals("p11 s1\n p2 s2\n p3 s3\n", FormattingUtils.alignLongStringsByRightSide(2, "p11", "s1", "p2", "s2", "p3", "s3"));
        assertEquals("p1 s11\np2  s2\np3  s3\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s11", "p2", "s2", "p3", "s3"));
        assertEquals(" p1 s1\np22 s2\n p3 s3\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p22", "s2", "p3", "s3"));
        assertEquals("p1  s1\np2 s22\np3  s3\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p2", "s22", "p3", "s3"));
        assertEquals(" p1 s1\n p2 s2\np33 s3\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p2", "s2", "p33", "s3"));
        assertEquals("p1  s1\np2  s2\np3 s33\n", FormattingUtils.alignLongStringsByRightSide(2, "p1", "s1", "p2", "s2", "p3", "s33"));

        // 3 Columns 3 Lines
        assertEquals("p1 m1 s1\np2 m2 s2\np3 m3 s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m2", "s2", "p3", "m3", "s3"));
        assertEquals("p11 m1 s1\n p2 m2 s2\n p3 m3 s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p11", "m1", "s1", "p2", "m2", "s2", "p3", "m3", "s3"));
        assertEquals("p1 m11 s1\np2  m2 s2\np3  m3 s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m11", "s1", "p2", "m2", "s2", "p3", "m3", "s3"));
        assertEquals("p1 m1 s11\np2 m2  s2\np3 m3  s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s11", "p2", "m2", "s2", "p3", "m3", "s3"));
        assertEquals(" p1 m1 s1\np22 m2 s2\n p3 m3 s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p22", "m2", "s2", "p3", "m3", "s3"));
        assertEquals("p1  m1 s1\np2 m22 s2\np3  m3 s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m22", "s2", "p3", "m3", "s3"));
        assertEquals("p1 m1  s1\np2 m2 s22\np3 m3  s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m2", "s22", "p3", "m3", "s3"));
        assertEquals(" p1 m1 s1\n p2 m2 s2\np33 m3 s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m2", "s2", "p33", "m3", "s3"));
        assertEquals("p1  m1 s1\np2  m2 s2\np3 m33 s3\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m2", "s2", "p3", "m33", "s3"));
        assertEquals("p1 m1  s1\np2 m2  s2\np3 m3 s33\n", FormattingUtils.alignLongStringsByRightSide(3, "p1", "m1", "s1", "p2", "m2", "s2", "p3", "m3", "s33"));
    }
}
