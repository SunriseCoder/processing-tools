package util;

import java.util.Arrays;
import java.util.List;

import process.ProcessRunnerToString;

public class YoutubeDLUtils {

    public static String getOTFManifestURL(String videoId) {
        List<String> command = Arrays.asList("youtube-dl", "-f", "bestvideo[ext=mp4]/bestvideo", "-g", "https://www.youtube.com/watch?v=" + videoId);

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
        List<String> command = Arrays.asList("youtube-dl", "-f", String.valueOf(iTag), "-g", "https://www.youtube.com/watch?v=" + videoId);

        ProcessRunnerToString runner = new ProcessRunnerToString();
        int exitCode = runner.execute(command);
        if (exitCode != 0) {
            throw new RuntimeException("External process exit code: " + exitCode);
        }
        String output = runner.getOutput().toString();

        String result = output.split("\n")[0];

        return result;
    }
}
