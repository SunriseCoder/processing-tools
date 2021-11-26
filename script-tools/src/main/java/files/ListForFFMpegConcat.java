package files;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;

import filters.ExclusiveFilenameFilter;
import helpers.FilesHelper;

public class ListForFFMpegConcat {

    public static void main(String[] args) throws IOException {
        System.out.println("Preparing video list for ffmpeg concat...");

        File currentFolder = new File(".");

        ExclusiveFilenameFilter filter = new ExclusiveFilenameFilter("bat", "txt");
        String[] filenames = currentFolder.list(filter);

        try (
                PrintWriter concatPrintWriter = new PrintWriter("files.txt");
                PrintWriter jointsPrintWriter = new PrintWriter("joints.txt");) {

            Duration totalDuration = Duration.ZERO;
            for (String filename : filenames) {
                // Files for FFMPEG
                String line = "file '" + filename + "'";
                System.out.println(line);
                concatPrintWriter.println(line);

                // Joint timings
                Duration duration = getVideoDuration(filename);
                jointsPrintWriter.println(format(totalDuration) + " - " + filename);
                totalDuration = totalDuration.plus(duration);
                System.out.println("Duration: " + format(duration) + ", Total: " + format(totalDuration));
            }
        }

        System.out.println("Done");
    }

    private static Duration getVideoDuration(String filename) {
        double rawDuration = FilesHelper.getVideoDuration(filename);
        long durationInMs = Math.round(rawDuration * 1000);
        Duration duration = Duration.ofMillis(durationInMs);
        return duration;
    }

    private static String format(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        String result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return result;
    }
}
