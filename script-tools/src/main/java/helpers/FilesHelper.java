package helpers;

import java.util.Arrays;
import java.util.List;

import process.ProcessRunnerToString;

public class FilesHelper {

    public static double getVideoDuration(String filename) {
        // TODO Auto-generated method stub
        List<String> command = Arrays.asList("ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=duration",
                "-of", "csv=p=0", filename);

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
