package app.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testTrimEndSymbols() {
        assertEquals(null, StringUtils.trimEndSymbols(null, null));
        assertEquals("", StringUtils.trimEndSymbols("", ""));

        assertEquals("text", StringUtils.trimEndSymbols("text", ""));

        assertEquals("tex", StringUtils.trimEndSymbols("text", "t"));
        assertEquals("tex", StringUtils.trimEndSymbols("texttt", "t"));

        assertEquals("text", StringUtils.trimEndSymbols("text\n", "\n"));
        assertEquals("text", StringUtils.trimEndSymbols("text\n\n\n", "\n"));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("Just Random Text For Test", StringUtils.toCamelCase("JUst Random tExt foR TesT"));
    }
}
