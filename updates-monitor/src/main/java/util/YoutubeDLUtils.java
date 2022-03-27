package util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import process.ProcessRunnerToFile;
import process.ProcessRunnerToString;

public class YoutubeDLUtils {

    public static String getOTFManifestURL(String videoId) {
        List<String> command = Arrays.asList("youtube-dl", "-f", "bestvideo[ext=mp4]/bestvideo",
                "--cookies", "youtube.com_cookies.txt",
                "-g", "https://www.youtube.com/watch?v=" + videoId);

        ProcessRunnerToString runner = new ProcessRunnerToString();
        int exitCode = runner.execute(command);
        if (exitCode != 0) {
            throw new RuntimeException("External process exit code: " + exitCode);
        }
        String output = runner.getOutput().toString();

        String result = output.split("\n")[0];

        return result;
    }

    public static String getDownloadURL(String videoId, int iTag) {
        List<String> command = Arrays.asList("youtube-dl", "-f", String.valueOf(iTag),
                "--cookies", "youtube.com_cookies.txt",
                "-g", "https://www.youtube.com/watch?v=" + videoId);

        ProcessRunnerToString runner = new ProcessRunnerToString();
        int exitCode = runner.execute(command);
        if (exitCode != 0) {
            throw new RuntimeException("External process exit code: " + exitCode);
        }
        String output = runner.getOutput().toString();

        String result = output.split("\n")[0];

        return result;
    }

    public static String getNonAdaptiveDownloadURL(String videoId) {
        List<String> command = Arrays.asList("youtube-dl", "-f", "best",
                //"--cookies", "youtube.com_cookies.txt",
                "-g", "https://www.youtube.com/watch?v=" + videoId);

        ProcessRunnerToString runner = new ProcessRunnerToString();
        int exitCode = runner.execute(command);
        if (exitCode != 0) {
            throw new RuntimeException("External process exit code: " + exitCode);
        }
        String output = runner.getOutput().toString();

        String result = output.split("\n")[0];

        return result;
    }

    public static boolean downloadVideo(String videoId, String formats, String resultFilename, String logSuffix) {
        List<String> command = Arrays.asList("youtube-dl", "-f", formats,
                //"--cookies", "youtube.com_cookies.txt",
                "-o", resultFilename,
                "https://www.youtube.com/watch?v=" + videoId);

        // Preparing OS process runner
        ProcessRunnerToFile processRunner = new ProcessRunnerToFile();
        processRunner.setOutputFile(new File("logs/youtube-dl-output-" + logSuffix + ".log"));
        processRunner.setErrorFile(new File("logs/youtube-dl-errors-" + logSuffix + ".log"));

        // Executing the OS process
        int exitCode = processRunner.execute(command);
        boolean result = exitCode == 0;
        return result;
    }
}
