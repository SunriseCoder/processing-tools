package util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utils.ProcessRunnerFileOutput;

public class FFMPEGUtils {

    public static synchronized boolean combineVideoAndAudio(String videoTrackPath, String audioTrackPath, String resultFilename) {
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
        ProcessRunnerFileOutput processRunner = new ProcessRunnerFileOutput();
        processRunner.setOutputFile(new File("ffmpeg-output.log"));
        processRunner.setErrorFile(new File("ffmpeg-errors.log"));

        // Executing the OS process
        int exitCode = processRunner.execute(command);
        boolean result = exitCode == 0;
        return result;
    }
}
