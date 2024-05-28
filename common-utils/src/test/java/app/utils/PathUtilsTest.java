package app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class PathUtilsTest {

    @Test
    public void testDeleteFolderRecursively() throws IOException {
        Path folder = Files.createTempDirectory("path-utils-test-");

        int width = 2;
        int depth = 3;
        int fileSize = 1024 * 1024;

        createFoldersRecursively(folder, 0, width, depth, fileSize);

        PathUtils.deleteFolderRecursively(folder);

        assertFalse(Files.exists(folder));
    }

    private void createFoldersRecursively(Path folder, int currentDepth, int width, int depth, int fileSize) throws IOException {
        if (currentDepth >= depth) {
            Path filePath = Files.createTempFile(folder, "temp-file-", ".tmp");
            //System.out.println("Creating File: " + filePath.toString());
            writeToFile(filePath, fileSize);
            return;
        }

        for (int i = 0; i < width; i++) {
            Path subFolder = Files.createTempDirectory(folder, "depth-" + currentDepth + "-");
            //System.out.println("Created Folder: " + subFolder.toString());
            createFoldersRecursively(subFolder, currentDepth + 1, width, depth, fileSize);
        }
    }

    private void writeToFile(Path filePath, int sizeOfData) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(filePath.toString())) {

            FileChannel outputChannel = outputStream.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(sizeOfData);
            Random rnd = new Random();
            rnd.nextBytes(buffer.array());
            outputChannel.write(buffer);
        }
    }

    @Test
    public void testSplitPathsFromString() {
        assertEquals(0, PathUtils.splitPathsFromString(null).size());
        assertEquals(0, PathUtils.splitPathsFromString("").size());

        List<Path> pathList = PathUtils.splitPathsFromString("1");
        assertEquals(1, pathList.size());
        assertEquals("1", pathList.get(0).toString());

        pathList = PathUtils.splitPathsFromString("1" + File.pathSeparator + "2");
        assertEquals(2, pathList.size());
        assertEquals("1", pathList.get(0).toString());
        assertEquals("2", pathList.get(1).toString());
    }
}
