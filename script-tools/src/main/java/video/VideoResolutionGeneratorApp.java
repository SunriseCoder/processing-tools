package video;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import process.ProcessRunnerToFile;
import wrappers.IntWrapper;

public class VideoResolutionGeneratorApp {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            printUsage();
            System.exit(-1);
        }

        long startTime = System.currentTimeMillis();

        VideoResolutionGenerator generator = new VideoResolutionGenerator();
        generator.setSourceFolder(args[0]);
        generator.setTargetFolder(args[1]);
        generator.setTargetResolutionFile(args[2]);

        Map<String, List<String>> convertCommands = generator.generateCommands();

        ProcessRunnerToFile processRunner = new ProcessRunnerToFile();
        processRunner.setOutputFile(new File("generate-output.log"));
        processRunner.setErrorFile(new File("generate-errors.log"));

        IntWrapper counter = new IntWrapper(1);
        convertCommands.entrySet().stream().forEach(command -> {
            long lastTime = System.currentTimeMillis();
            System.out.println("Processing " + counter.postIncrement() + " of " + convertCommands.size() + ": " + command.getKey() + "...");
            System.out.println("Executing: " + commandToString(command.getValue()));
            processRunner.execute(command.getValue());
            System.out.println("\tGenerating took " + (System.currentTimeMillis() - lastTime) + " ms");
        });

        System.out.println("Generation done, took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private static void printUsage() {
        System.out.println("Usage: " + VideoResolutionGeneratorApp.class.getName() + " <source-folder> <target-folder> <target-resolutions-file>\n"
                + "\t where\n"
                + "\t\t <source-folder> is a folder with original files to be converted\n"
                + "\t\t <target-folder> is a folder, where new generated files should be saved\n"
                + "\t\t <target-resolutions-file> is a file, containing all required resolutions\n");
    }

    private static String commandToString(List<String> command) {
        String result = command.stream().collect(Collectors.joining(" "));
        return result;
    }
}
