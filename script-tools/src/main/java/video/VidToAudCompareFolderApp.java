package video;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import filters.FilenameFilterVideos;
import utils.FileUtils;

public class VidToAudCompareFolderApp {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(-1);
        }

        File sourceFolder = new File(args[0]);
        File targetFolder = new File(args[1]);

        FilenameFilterVideos filter = new FilenameFilterVideos();
        String[] sourceFileNames = sourceFolder.list(filter);
        Set<String> targetFiles = new HashSet<>(Arrays.asList(targetFolder.list()));

        ProcessRunnerFileOutput processRunner = new ProcessRunnerFileOutput();
        processRunner.setOutputFile(new File("generate-output.log"));
        processRunner.setErrorFile(new File("generate-errors.log"));

        for (String sourceFileName : sourceFileNames) {
            String targetFileName = FileUtils.replaceResolution(sourceFileName, "mp3");
            if (targetFiles.contains(targetFileName)) {
                continue;
            }

            System.out.println("Processing " + sourceFileName);

            String sourceFilePath = new File(sourceFolder, sourceFileName).getAbsolutePath();
            String targetFilePath = new File(targetFolder, targetFileName).getAbsolutePath();
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

    private static void printUsage() {
        System.out.println("Usage: " + VidToAudCompareFolderApp.class.getName() + " <source-folder> <target-folder>\n"
                + "\t where\n"
                + "\t\t <source-folder> is a folder with original files to be converted\n"
                + "\t\t <target-folder> is a folder, where new generated files should be saved\n");
    }
}
