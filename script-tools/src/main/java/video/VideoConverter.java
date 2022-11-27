package video;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoConverter {
    private String sourceFile;
    private String resolution;
    private String frequency;
    private String channels;
    private String fps;
    private String silence;
    private String targetFile;

    public void setSourceFile(File file) {
        this.sourceFile = file.getAbsolutePath();
    }

    public void setResolution(String resolution) {
        this.resolution = resolution.replace("x", ":");
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setAudioChannels(String channels) {
        this.channels = channels;
    }

    public void setFPS(String fps) {
        this.fps = fps;
    }

    public void setSilence(String silence) {
        this.silence = silence;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile.getAbsolutePath();
    }

    public List<String> generateCommand() {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-loglevel");
        command.add("info");

        // Input file
        command.add("-i");
        command.add(sourceFile);
        // Video settings
        command.add("-c:v");
        command.add("libx264");
        command.add("-crf");
        command.add("23");
        command.add("-vsync");
        command.add("vfr");
        command.add("-video_track_timescale");
        command.add("90000");
        command.add("-vf");
        command.add("\"scale=" + resolution + ",fps=" + fps + "\"");
        // Audio settings
        command.add("-c:a");
        command.add("aac");
        command.add("-ar");
        command.add(frequency);
        command.add("-ac");
        command.add(channels);

        // Silence command
        if (silence != null && !silence.isEmpty()) {
            command.addAll(Arrays.asList(silence.split(" ")));
        }

        // Output file
        command.add("\"" + targetFile + "\"");

        return command;
    }
}
