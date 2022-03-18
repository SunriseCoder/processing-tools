package video;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import filters.FilenameFilterVideos;
import utils.FileUtils;
import utils.ProcessRunnerFileOutput;

public class VidToAudCompareFolderApp {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(-1);
        }

        FolderScanner folderScanner = new FolderScanner();

        File sourceFolder = new File(args[0]);
        folderScanner.setSourceFolder(sourceFolder);
        File targetFolder = new File(args[1]);
        folderScanner.setTargetFolder(targetFolder);

        FilenameFilterVideos filter = new FilenameFilterVideos(true);
        folderScanner.setFilenameFilter(filter);

        folderScanner.scan();

        System.out.println("Done");
    }

    private static void printUsage() {
        System.out.println("Usage: " + VidToAudCompareFolderApp.class.getName() + " <source-folder> <target-folder>\n"
                + "\t where\n"
                + "\t\t <source-folder> is a folder with original files to be converted\n"
                + "\t\t <target-folder> is a folder, where new generated files should be saved\n");
    }

    private static class FolderScanner {
        private File sourceFolder;
        private String sourceFolderPath;
        private File targetFolder;
        private FilenameFilter filenameFilter;

        private ProcessRunnerFileOutput processRunner;

        public void setSourceFolder(File sourceFolder) {
            this.sourceFolder = sourceFolder;
            this.sourceFolderPath = sourceFolder.getAbsolutePath();
        }

        public void setTargetFolder(File targetFolder) {
            this.targetFolder = targetFolder;
        }

        public void setFilenameFilter(FilenameFilter filenameFilter) {
            this.filenameFilter = filenameFilter;
        }

        public void scan() {
            processRunner = new ProcessRunnerFileOutput();
            processRunner.setOutputFile(new File("generate-output.log"));
            processRunner.setErrorFile(new File("generate-errors.log"));

            scanFolder(sourceFolder);
        }

        private void scanFolder(File folder) {
            for (File file : folder.listFiles(filenameFilter)) {
                if (file.isDirectory()) {
                    scanFolder(file);
                    continue;
                }

                String sourceFilePath = file.getAbsolutePath();
                String sourceRelativePath = sourceFilePath.substring(sourceFolderPath.length());
                String targetRelativePath = FileUtils.replaceFileExtension(sourceRelativePath, "mp3");
                File targetFile = new File(targetFolder, targetRelativePath);

                if (targetFile.exists()) {
                    System.out.println("File \"" + sourceRelativePath + "\" exists, skipping...");
                    continue;
                } else if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }

                System.out.println("Processing file: " + sourceRelativePath);

                String targetFilePath = targetFile.getAbsolutePath();
                List<String> command = generateCommand(sourceFilePath, targetFilePath);
                processRunner.execute(command);
            }
        }

        private static List<String> generateCommand(String sourceFilePath, String targetFilePath) {
            List<String> command = new ArrayList<>();
            command.add("ffmpeg");

            // Input file
            command.add("-i");
            command.add("\"" + sourceFilePath + "\"");

            // Audio settings
            command.add("-ac");
            command.add("1");
            command.add("-ar");
            command.add("22050");
            command.add("-q:a");
            command.add("9");

            // Output file
            command.add("\"" + targetFilePath + "\"");

            return command;
        }
    }
}
