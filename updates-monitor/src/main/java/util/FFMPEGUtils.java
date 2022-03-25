package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.dto.youtube.YoutubeDownloadDetails;
import process.ProcessRunnerToFile;
import process.ProcessRunnerToString;

public class FFMPEGUtils {

    public static boolean combineVideoAndAudio(String videoTrackPath,
            String audioTrackPath, String resultFilename, String videoId) {
        List<String> command = new ArrayList<>();

        // Base settings
        command.add("ffmpeg");
        command.add("-loglevel");
        command.add("info");
        command.add("-y");

        // Input files
        command.add("-i");
        command.add("\"" + videoTrackPath + "\"");
        command.add("-i");
        command.add("\"" + audioTrackPath + "\"");

        // Copy settings
        command.add("-c");
        command.add("copy");

        // Output file
        command.add("\"" + resultFilename + "\"");

        // Preparing OS process runner
        ProcessRunnerToFile processRunner = new ProcessRunnerToFile();
        processRunner.setOutputFile(new File("logs/ffmpeg-output-" + videoId + ".log"));
        processRunner.setErrorFile(new File("logs/ffmpeg-errors-" + videoId + ".log"));

        // Executing the OS process
        int exitCode = processRunner.execute(command);
        boolean result = exitCode == 0;
        return result;
    }

    public static boolean muxMPDMManifest(String manifestFilename, String resultFilename,
            YoutubeDownloadDetails downloadDetails) {
        List<String> command = new ArrayList<>();

        // Base settings
        command.add("ffmpeg");
        command.add("-loglevel");
        command.add("info");
        command.add("-y");

        // Input files
        command.add("-i");
        command.add("\"" + manifestFilename + "\"");

        // Mapping
        command.add("-map");
        command.add("0:m:id:" + downloadDetails.getVideoFormat().iTag);
        command.add("-map");
        command.add("0:m:id:" + downloadDetails.getAudioFormat().iTag);

        // Copy settings
        command.add("-c");
        command.add("copy");

        // Output file
        command.add("\"" + resultFilename + "\"");

        // Preparing OS process runner
        ProcessRunnerToFile processRunner = new ProcessRunnerToFile();
        processRunner.setOutputFile(new File("logs/ffmpeg-output-" + downloadDetails.getVideoId() + ".log"));
        processRunner.setErrorFile(new File("logs/ffmpeg-errors-" + downloadDetails.getVideoId() + ".log"));

        // Executing the OS process
        int exitCode = processRunner.execute(command);
        boolean result = exitCode == 0;
        return result;
    }

    public static boolean makePreview(String sourceVideo, String resultFilePrefix, int interval) {
        List<String> command = new ArrayList<>();

        // Base settings
        command.add("ffmpeg");
        command.add("-loglevel");
        command.add("info");
        command.add("-y");

        // Input files
        command.add("-i");
        command.add("\"" + sourceVideo + "\"");

        // Settings
        command.add("-vf");
        command.add("fps=1/" + interval + ",scale=320:-1");

        // Output file
        command.add("\"" + resultFilePrefix + "%04d.jpg" + "\"");

        // Preparing OS process runner
        ProcessRunnerToFile processRunner = new ProcessRunnerToFile();
        processRunner.setOutputFile(new File("logs/ffmpeg-output-preview.log"));
        processRunner.setErrorFile(new File("logs/ffmpeg-errors.log"));

        // Executing the OS process
        int exitCode = processRunner.execute(command);
        boolean result = exitCode == 0;

        return result;
    }

    public static double getVideoDuration(String filename) {
        List<String> command = Arrays.asList("ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=duration",
                "-of", "csv=p=0", "\"" + filename + "\"");

        // Preparing OS process runner
        ProcessRunnerToString runner = new ProcessRunnerToString();
        int exitCode = runner.execute(command);
        if (exitCode != 0) {
            throw new RuntimeException("External process exit code: " + exitCode);
        }

        String processOutput = runner.getOutput().toString();
        String stringValue = processOutput.split("\n")[0];
        double result = Double.parseDouble(stringValue);

        return result;
    }
}
