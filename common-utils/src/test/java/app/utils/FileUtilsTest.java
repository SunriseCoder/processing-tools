package app.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FileUtilsTest {

    @Test
    public void testGetFileExtension() {
        assertEquals("txt", FileUtils.getFileExtension("file.txt"));
    }

    @Test
    public void testGetFileExtensionWithNoDot() {
        assertEquals("filetxt", FileUtils.getFileExtension("filetxt"));
    }

    @Test
    public void testGetFileExtensionWithNoName() {
        assertEquals("filetxt", FileUtils.getFileExtension(".filetxt"));
    }

    @Test
    public void testGetFileName() {
        assertEquals("file", FileUtils.getFileName("file.txt"));
    }

    @Test
    public void testGetFileNameWithNoName() {
        assertEquals("", FileUtils.getFileName(".txt"));
    }

    @Test
    public void testDriveLetterToUpperCaseIfNeeded() {
        assertEquals(null, FileUtils.driveLetterToUpperCaseIfNeeded(null));
        assertEquals("", FileUtils.driveLetterToUpperCaseIfNeeded(""));
        assertEquals("a", FileUtils.driveLetterToUpperCaseIfNeeded("a"));
        assertEquals("A:", FileUtils.driveLetterToUpperCaseIfNeeded("a:"));
        assertEquals("C:", FileUtils.driveLetterToUpperCaseIfNeeded("c:"));
        assertEquals("W:", FileUtils.driveLetterToUpperCaseIfNeeded("w:"));
        assertEquals("A:", FileUtils.driveLetterToUpperCaseIfNeeded("A:"));
        assertEquals("A:\\file.txt", FileUtils.driveLetterToUpperCaseIfNeeded("a:\\file.txt"));
        assertEquals("A:\\Folder\\File.txt", FileUtils.driveLetterToUpperCaseIfNeeded("a:\\Folder\\File.txt"));
        assertEquals("A:\\Folder\\File.txt", FileUtils.driveLetterToUpperCaseIfNeeded("A:\\Folder\\File.txt"));
    }
}
