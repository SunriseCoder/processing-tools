package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MakeApplicationBuildApp {

    /*
     * @param args
     * 0 - artifact-id
     * 1 - digits in version counter
     * 2 - base folder

     * 3-n - files to copy (jar-files, config-files, etc)
     * */
    public static void main(String[] args) throws IOException {
        System.out.println("Starting...");

        // Parsing Input Parameters
        String artifactId = args[0];
        int digitsInVersionCounter = Integer.parseInt(args[1]);
        Path baseFolder = Paths.get(args[2]);

        // Defining next version number
        long lastVersionNumber = findLastVersionNumber(baseFolder, artifactId);
        String newVersionNumberString = fillNumberWithBeginningZeros(lastVersionNumber + 1, digitsInVersionCounter);
        String newVersionFolderName = artifactId + "-v" + newVersionNumberString;
        System.out.println("Creating new version build: " + newVersionFolderName);

        // Creating new folder
        Path newVersionFolderPath = baseFolder.resolve(newVersionFolderName);
        Files.createDirectory(newVersionFolderPath);

        // Copy files
        System.out.println("Starting Copy files...");
        for (int i = 3; i < args.length; i++) {
            Path fileSourcePath = Paths.get(args[i]);
            Path fileDestinationPath = newVersionFolderPath.resolve(fileSourcePath.getFileName());
            copyFile(fileSourcePath, fileDestinationPath);
        }

        // Generating bash-files
        generateBashFile(artifactId, null, 9, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBashFile("snapshot-maker", "app.SnapshotMakerApp", 1, artifactId, newVersionNumberString, newVersionFolderPath);

        // Generating bat-files
        generateBatFile(artifactId, null, 9, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("snapshot-maker", "app.SnapshotMakerApp", 1, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("snapshot-dump", "app.SnapshotDumpApp", 1, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("snapshot-checker", "app.SnapshotCheckerApp", 2, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("snapshot-assembler", "app.SnapshotAssemblerApp", 2, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("file-database-maker", "app.MakeFileDatabaseApp", 9, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("folder-sorter", "app.FolderSortingApp", 1, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("folder-sorter-validator", "app.FolderSortingValidationApp", 1, artifactId, newVersionNumberString, newVersionFolderPath);
        generateBatFile("search-file-duplications", "app.SearchFileDuplicationsApp", 2, artifactId, newVersionNumberString, newVersionFolderPath);

        // Packing ZIP-file
        System.out.println("Creating Zip-file...");
        Path zipFilePath = Paths.get(newVersionFolderPath.toString() + ".zip");
        zipFolder(newVersionFolderPath, zipFilePath);

        // Copy build folder to c:\portable
        System.out.println("Copying build folder to c:\\portable...");
        Path newVersionFolderInPortable = Paths.get("C:\\portable").resolve(newVersionFolderName);
        copyFolder(newVersionFolderPath, newVersionFolderInPortable);

        // Copy working data from previous build in c:\portable
        String lastVersionNumberString = fillNumberWithBeginningZeros(lastVersionNumber, digitsInVersionCounter);
        String lastVersionFolderName = artifactId + "-v" + lastVersionNumberString;
        Path lastVersionFolderInPortable = Paths.get("C:\\portable").resolve(lastVersionFolderName);
        copyResourceFromPreviousVersion("configuration.json", lastVersionFolderInPortable, newVersionFolderInPortable);
        copyResourceFromPreviousVersion("file-database.json", lastVersionFolderInPortable, newVersionFolderInPortable);

        System.out.println("Application has finished");
    }

    private static long findLastVersionNumber(Path baseFolder, String artifactId) throws IOException {
        Pattern buildFolderPattern = Pattern.compile("^" + artifactId + "-v([0-9]+)$");
        long lastBuildVersion = 0;

        DirectoryStream<Path> baseFolderStream = Files.newDirectoryStream(baseFolder);
        for (Path filePath : baseFolderStream) {
            Matcher matcher = buildFolderPattern.matcher(filePath.getFileName().toString());
            if (matcher.matches()) {
                long versionFromFolderName = Long.parseLong(matcher.group(1));
                if (versionFromFolderName > lastBuildVersion) {
                    lastBuildVersion = versionFromFolderName;
                }
            }
        }

        return lastBuildVersion;
    }

    private static String fillNumberWithBeginningZeros(long value, int targetLength) {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        while (sb.length() < targetLength) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }

    private static void generateBashFile(String bashFilePrefix, String mainClass, int amountOfArguments,
            String artifactId, String version, Path folderPath) throws FileNotFoundException {
        File bashFile = folderPath.resolve(bashFilePrefix + "-v" + version + ".sh").toFile();
        try (PrintWriter pw = new PrintWriter(bashFile)) {
            pw.println("#!/bin/bash -x");
            pw.println("");
            pw.println("apphome=$(dirname $(realpath \"$0\"))");
            pw.println("echo \"apphome is: $apphome\"");
            if (mainClass == null) {
                pw.println("java -Dapp.home=\"$apphome\" -jar \"$apphome/" + artifactId + "-0.0.1-SNAPSHOT-jar-with-dependencies.jar\" "
                        + generateBashFileArgs(amountOfArguments));
            } else {
                pw.println("java -Dapp.home=\"$apphome\" -cp \"$apphome/" + artifactId + "-0.0.1-SNAPSHOT-jar-with-dependencies.jar\" " + mainClass
                        + " " + generateBashFileArgs(amountOfArguments));
            }

        }
    }

    private static String generateBashFileArgs(int amountOfArguments) {
        List<String> strings = new ArrayList<>();
        for (int i = 1; i <= amountOfArguments; i++) {
            strings.add("$" + i);
        }
        String result = String.join(" ", strings);
        return result;
    }

    private static void generateBatFile(String batFilePrefix, String mainClass, int amountOfArguments,
            String artifactId, String version, Path folderPath) throws FileNotFoundException {
        File batFile = folderPath.resolve(batFilePrefix + "-v" + version + ".bat").toFile();
        try (PrintWriter pw = new PrintWriter(batFile)) {
            pw.println("set apphome=\"C:\\portable\\" + artifactId + "-v" + version + "\"");
            if (mainClass == null) {
                pw.println("java -Dapp.home=%apphome% -jar %apphome%\\" + artifactId + "-0.0.1-SNAPSHOT-jar-with-dependencies.jar "
                        + generateBatFileArgs(amountOfArguments));
            } else {
                pw.println("java -Dapp.home=%apphome% -cp %apphome%\\" + artifactId + "-0.0.1-SNAPSHOT-jar-with-dependencies.jar " + mainClass
                        + " " + generateBatFileArgs(amountOfArguments));
            }
            pw.println("pause");
        }
    }

    private static String generateBatFileArgs(int amountOfArguments) {
        List<String> strings = new ArrayList<>();
        for (int i = 1; i <= amountOfArguments; i++) {
            strings.add("%" + i);
        }
        String result = String.join(" ", strings);
        return result;
    }

    private static void zipFolder(Path sourceFolder, Path zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
                ZipOutputStream zipOut = new ZipOutputStream(fos);) {
            File folderToZip = sourceFolder.toFile();
            zipFile(folderToZip, folderToZip.getName(), zipOut);
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToZip);) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }

    private static void copyFolder(Path sourceFolder, Path destinationFolder) throws IOException {
        System.out.println("Copying folder " + sourceFolder + " to " + destinationFolder);
        Iterator<Path> pathIterator = Files.walk(sourceFolder).iterator();
        while (pathIterator.hasNext()) {
            Path sourcePath = pathIterator.next();
            Path sourceRelativePath = sourceFolder.relativize(sourcePath);
            Path destinationPath = destinationFolder.resolve(sourceRelativePath);
            System.out.println("  Copying file " + sourcePath + " to " + destinationPath);
            Files.copy(sourcePath, destinationPath);
        }
    }

    private static void moveFile(Path source, Path destination) throws IOException {
        System.out.println("Moving " + source + " to " + destination);
        Files.move(source, destination);
    }

    private static void copyFile(Path source, Path destination) throws IOException {
        System.out.println("Copying " + source + " to " + destination);
        Files.copy(source, destination);
    }

    private static void copyResourceFromPreviousVersion(String resourceToCopy,
            Path oldVersionFolderInPortable, Path newVersionFolderInPortable) throws IOException {
        Path newVersionResourceFilePath = newVersionFolderInPortable.resolve(resourceToCopy);
        Path oldVersionResourceFilePath = oldVersionFolderInPortable.resolve(resourceToCopy);
        if (Files.notExists(oldVersionResourceFilePath)) {
            System.out.println("Resource file does not exists, skipping: " + newVersionResourceFilePath.toString());
            return;
        }
        String resourceToCopyBackup = getFileName(resourceToCopy) + ".backup." + getFileExtension(resourceToCopy);
        Path newVersionResourceBackupFilePath = newVersionFolderInPortable.resolve(resourceToCopyBackup);
        if (Files.exists(newVersionResourceFilePath)) {
            moveFile(newVersionResourceFilePath, newVersionResourceBackupFilePath);
        }
        copyFile(oldVersionResourceFilePath, newVersionResourceFilePath);
    }

    private static String getFileExtension(String filename) {
        int positionOfLastDot = filename.lastIndexOf(".");
        String format = filename.substring(positionOfLastDot + 1);
        return format;
    }

    private static String getFileName(String filename) {
        int positionOfLastDot = filename.lastIndexOf(".");
        String name = filename.substring(0, positionOfLastDot);
        return name;
    }
}
