package video;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;

import utils.JSONUtils;

public class VideoResolutionGenerator {
    // CA_Intro_1080x1920-60fps.mp4
    private static final Pattern FILE_PATTERN = Pattern.compile("^([A-z]*)([0-9]+)x([0-9]+)-([0-9]+)fps.mp4$");

    private File sourceFolder;
    private File targetFolder;
    private File targetResolutionsFile;

    private List<FileParameters> originalFileParameters;
    private List<TargetParameters> targetParameterGroups;
    private Map<String, List<String>> convertCommands;

    public void setSourceFolder(String filename) {
        sourceFolder = new File(filename);
    }

    public void setTargetFolder(String filename) {
        targetFolder = new File(filename);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
    }

    public void setTargetResolutionFile(String filename) {
        targetResolutionsFile = new File(filename);
    }

    public Map<String, List<String>> generateCommands() throws IOException {
        getSourceFiles();
        getTargetResolutions();
        generateConvertCommands();
        return convertCommands;
    }

    private void getSourceFiles() {
        originalFileParameters = new ArrayList<>();
        File[] sourceFiles = sourceFolder.listFiles();
        for (File sourceFile : sourceFiles) {
            String sourceFileName = sourceFile.getName();

            Matcher matcher = FILE_PATTERN.matcher(sourceFileName);
            if (!matcher.matches()) {
                System.out.println("File " + sourceFileName + " doesn't match the Pattern " + FILE_PATTERN.toString()
                        + ", skipping...");
                continue;
            }

            int resolutionX = Integer.parseInt(matcher.group(2));
            int resolutionY = Integer.parseInt(matcher.group(3));
            double aspectRatio = (double) resolutionX / resolutionY;

            FileParameters parameters = new FileParameters();
            parameters.file = sourceFile;
            parameters.prefix = matcher.group(1);
            parameters.aspectRatio = aspectRatio;
            originalFileParameters.add(parameters);
        }
    }

    private void getTargetResolutions() throws IOException {
        TypeReference<List<TargetParameters>> typeReference = new TypeReference<List<TargetParameters>>() { };
        targetParameterGroups = JSONUtils.loadFromDisk(targetResolutionsFile, typeReference);
        System.out.println("Target resolutions has been loaded successfully.");
    }

    private void generateConvertCommands() {
        convertCommands = new LinkedHashMap<>();
        VideoConverter converter = new VideoConverter();
        for (TargetParameters targetParameters : targetParameterGroups) {
            System.out.println("Generating group: " + targetParameters.name);
            for (String prefix : targetParameters.prefixes) {
                for (String resolution : targetParameters.resolutions) {
                    converter.setResolution(resolution);

                    FileParameters sourceFileParameters = getCloserSourceFile(prefix, resolution);
                    converter.setSourceFile(sourceFileParameters.file);

                    for (String frequency : targetParameters.audioFrequences) {
                        converter.setFrequency(frequency);

                        for (String channel : targetParameters.audioChannels) {
                            converter.setAudioChannels(channel);

                            for (String fps : targetParameters.videoFPS) {
                                converter.setFPS(fps);

                                for (Map.Entry<String, String> silence : targetParameters.silence.entrySet()) {
                                    converter.setSilence(silence.getValue());

                                    String targetFileName = prefix + resolution + "-" + fps + "fps-" + frequency + "Hz-"
                                            + channel + "Ch-" + silence.getKey() + ".mp4";
                                    if (!convertCommands.containsKey(targetFileName)) {
                                        File targetFile = new File(targetFolder, targetFileName);
                                        converter.setTargetFile(targetFile);

                                        if (targetFile.exists()) {
                                            System.out.println("Skipping file " + targetFileName + ", because it's already exist.");
                                            continue;
                                        }

                                        List<String> command = converter.generateCommand();
                                        convertCommands.put(targetFileName, command);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private FileParameters getCloserSourceFile(String prefix, String resolution) {
        double targetAspectRatio = calculateAspectRatio(resolution);

        FileParameters lastParameters = null;
        double lastDelta = Double.MAX_VALUE;
        for (FileParameters parameters : originalFileParameters) {
            if (!parameters.prefix.equals(prefix)) {
                continue;
            }

            double delta = Math.abs(targetAspectRatio - parameters.aspectRatio);
            if (delta < lastDelta) {
                lastParameters = parameters;
                lastDelta = delta;
            }
        }

        return lastParameters;
    }

    private double calculateAspectRatio(String resolution) {
        String[] parts = resolution.split("x");
        int width = Integer.parseInt(parts[0]);
        int height = Integer.parseInt(parts[1]);
        double aspectRatio = (double) width / height;
        return aspectRatio;
    }

    private static class FileParameters {
        private File file;
        private String prefix;
        private double aspectRatio;
    }

    private static class TargetParameters {
        private String name;
        private List<String> prefixes;
        private List<String> resolutions;
        private List<String> audioFrequences;
        private List<String> audioChannels;
        private List<String> videoFPS;
        private Map<String, String> silence;
    }
}
