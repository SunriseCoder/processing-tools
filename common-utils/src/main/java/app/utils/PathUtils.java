package app.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.files.PathIterator;
import app.progress.ProgressPrinter;

public class PathUtils {
    private static final Logger LOGGER = LogManager.getLogger(PathUtils.class);

    public static Path checkAndGetFolder(String absoluteFolderName) throws IOException {
        Path path = Paths.get(absoluteFolderName).toAbsolutePath();
        if (Files.notExists(path)) {
            throw new FileNotFoundException(absoluteFolderName);
        } else if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(absoluteFolderName);
        }
        return path;
    }

    public static void createFolderIfNotExistsIncludingParents(Path path) throws IOException {
        Files.createDirectories(path);
    }

    public static Path getOrCreateFolder(Path absoluteFolderPath) throws IOException {
        if (Files.notExists(absoluteFolderPath)) {
            Files.createDirectories(absoluteFolderPath);
        } else if (!Files.isDirectory(absoluteFolderPath)) {
            throw new NotDirectoryException(absoluteFolderPath.toString());
        }
        return absoluteFolderPath;
    }

    public static void copyFile(Path source, Path destination, long copyChunkSize, ProgressPrinter progressPrinter)
            throws IOException {
        LOGGER.debug("Starting to Copy File: " + source.toString() + " to " + destination.toString());

        PathUtils.createAllParentFolders(destination);

        long fileSize = Files.size(source);
        long readBytesFromCurrentFile = 0;
        try (FileInputStream inputStream = new FileInputStream(source.toString());
                FileOutputStream outputStream = new FileOutputStream(destination.toString());
                FileChannel inputChannel = inputStream.getChannel();
                FileChannel outputChannel = outputStream.getChannel();) {

            while (readBytesFromCurrentFile < fileSize) {
                long transfered = outputChannel.transferFrom(inputChannel, readBytesFromCurrentFile, copyChunkSize);
                if (progressPrinter != null) {
                    progressPrinter.addProgress(transfered);
                }
                readBytesFromCurrentFile += transfered;
            }
        }

        copyAttributes(source, destination);
    }

    public static void copyAttributes(Path source, Path destination) throws IOException {
        BasicFileAttributes attributes = readAttributes(source);
        Files.setAttribute(destination, "creationTime", attributes.creationTime());
        Files.setAttribute(destination, "lastModifiedTime", attributes.lastModifiedTime());
        Files.setAttribute(destination, "lastAccessTime", attributes.lastAccessTime());
    }

    public static void moveFolderWithReplacement(Path source, Path destination) throws IOException {
        createAllParentFolders(destination);
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void moveFileWithReplacement(Path source, Path destination) throws IOException {
        createAllParentFolders(destination);
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void deleteFile(Path path) throws IOException {
        Files.delete(path);
    }

    public static void deleteIfExists(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    public static void copyFolder(Path sourceFolder, Path destinationFolder,
            long copyChunkSize, ProgressPrinter progressPrinter) throws IOException {
        LOGGER.debug("Starting to Copy Folder " + sourceFolder.toString() + " to " + destinationFolder.toString());

        Iterator<Path> pathIterator = Files.walk(sourceFolder).iterator();
        while (pathIterator.hasNext()) {
            Path sourcePath = pathIterator.next();

            Path sourceRelativePath = sourceFolder.relativize(sourcePath);
            Path destinationPath = destinationFolder.resolve(sourceRelativePath);

            if (isDirectory(sourcePath)) {
                LOGGER.debug("Creating Folder: " + destinationPath.toString());
                createFolderIfNotExistsIncludingParents(destinationPath);
            } else {
                LOGGER.debug("Copying File: " + sourcePath.toString() + " to " + destinationPath.toString());
                copyFile(sourcePath, destinationPath, copyChunkSize, progressPrinter);
            }
        }
    }

    public static void createAllParentFolders(Path path) throws IOException {
        Files.createDirectories(path.getParent());
    }

    public static Path getAbsolutePathWithDriveLetterUpperCase(String pathString) {
        Path path = Paths.get(pathString);
        path = path.toAbsolutePath();
        path = path.normalize();
        String absolutePathString = path.toString();
        absolutePathString = FileUtils.driveLetterToUpperCaseIfNeeded(absolutePathString);
        path = Paths.get(absolutePathString);
        return path;
    }

    public static void deleteFolderRecursively(Path folder) throws IOException {
        LOGGER.debug("Starting to delete Folder " + folder.toString() + " recursively...");

        try (Stream<Path> stream = Files.walk(folder)) {
            Stream<Path> sortedStream = stream.sorted(Comparator.reverseOrder());
            Iterator<Path> pathIterator = sortedStream.iterator();
            while (pathIterator.hasNext()) {
                Path path = pathIterator.next();
                LOGGER.debug("Deleting Element: " + path.toString());
                deleteFile(path);
            }
        }
    }

    public static long countFolderSize(Path source) throws IOException {
        long folderSize = new PathIterator(source, true).stream()
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).sum();
        return folderSize;
    }

    public static BasicFileAttributes readAttributes(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return attributes;
    }

    public static boolean exists(Path path) {
        boolean result = Files.exists(path);
        return result;
    }

    public static boolean notExists(Path path) {
        boolean result = Files.notExists(path);
        return result;
    }

    public static boolean isDirectoryEmpty(Path path) throws IOException {
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
            boolean result = !directory.iterator().hasNext();
            return result;
        }
    }

    public static boolean isDirectory(Path path) {
        boolean result = Files.isDirectory(path);
        return result;
    }

    public static boolean isNotDirectory(Path path) {
        boolean result = !Files.isDirectory(path);
        return result;
    }

    public static boolean isSameDrive(Path path1, Path path2) {
        String path1String = path1.getRoot().toString().toUpperCase();
        String path2String = path2.getRoot().toString().toUpperCase();
        boolean result = path1String.equals(path2String);
        return result;
    }

    public static void moveFolderSmart(Path sourceFolder, Path destinationFolder, Path temporaryFolder,
            long copyChunkSize, ProgressPrinter progressPrinter) throws IOException {
        LOGGER.debug("Starting Smart Move of the folders: " + sourceFolder.toString() + " to " + destinationFolder.toString());

        sourceFolder = sourceFolder.toAbsolutePath().normalize();
        destinationFolder = destinationFolder.toAbsolutePath().normalize();

        if (notExists(sourceFolder)) {
            throw new FileNotFoundException("Source Folder does not exist: " + sourceFolder.toString());
        }

        if (isNotDirectory(sourceFolder)) {
            throw new NotDirectoryException("Source is not a Directory: " + sourceFolder.toString());
        }

        if (exists(destinationFolder) && isNotDirectory(destinationFolder)) {
            throw new NotDirectoryException("Destination is not a Directory: " + destinationFolder.toString());
        }

        if (notExists(destinationFolder) && notExists(destinationFolder.getRoot())) {
            throw new FileNotFoundException("Could not find FileSystem Root for the destination folder: " + destinationFolder.toString());
        }

        boolean isMoveWithinSameDrive = isSameDrive(sourceFolder, destinationFolder);
        // If Move is within the same disk
        if (isMoveWithinSameDrive) {
            LOGGER.debug("Moving is Within the Same Drive");
            // Checking that Destination Folder doesn't exist or empty -> then we can just Move the Folder by Replacement
            if (notExists(destinationFolder) || (exists(destinationFolder) && isDirectoryEmpty(destinationFolder))) {
                PathUtils.moveFolderWithReplacement(sourceFolder, destinationFolder);
            } else {
                // Destination Exists and Non-Empty Directory - Move all Children elements One-by-One
                moveFolderChildrenOneByOneWithinSameDriveOnly(sourceFolder, destinationFolder);
            }
        // Move from One disk to Another
        } else {
            LOGGER.debug("Moving is between Different Drives");
            // Creating Temporary Folder if not set
            if (temporaryFolder == null) {
                temporaryFolder = destinationFolder.getRoot().resolve("tmp");
                PathUtils.createFolderIfNotExistsIncludingParents(temporaryFolder);
                temporaryFolder = Files.createTempDirectory(temporaryFolder, "move-folder-tmp-");
            }

            try {
                copyFolder(sourceFolder, temporaryFolder, copyChunkSize, progressPrinter);

                if (notExists(destinationFolder) || (exists(destinationFolder) && isDirectoryEmpty(destinationFolder))) {
                    PathUtils.moveFolderWithReplacement(temporaryFolder, destinationFolder);
                } else {
                    // Destination Exists and Non-Empty Directory - Move all Children elements One-by-One
                    moveFolderChildrenOneByOneWithinSameDriveOnly(temporaryFolder, destinationFolder);
                }

                LOGGER.debug("Deleting Source Folder: " + sourceFolder.toString());
                deleteFolderRecursively(sourceFolder);
            } finally {
                if (exists(temporaryFolder)) {
                    LOGGER.debug("Deleting Temporary Folder: " + temporaryFolder.toString());
                    deleteFolderRecursively(temporaryFolder);
                }
            }
        }
    }

    public static void moveFolderChildrenOneByOneWithinSameDriveOnly(Path sourceFolder, Path destinationFolder) throws IOException {
        Iterator<Path> pathIterator = Files.walk(sourceFolder).iterator();
        while (pathIterator.hasNext()) {
            Path sourcePath = pathIterator.next();

            Path sourceRelativePath = sourceFolder.relativize(sourcePath);
            Path destinationPath = destinationFolder.resolve(sourceRelativePath);

            if (isDirectory(sourcePath)) {
                createFolderIfNotExistsIncludingParents(destinationPath);
            } else {
                moveFileWithReplacement(sourcePath, destinationPath);
            }
        }
    }
}
