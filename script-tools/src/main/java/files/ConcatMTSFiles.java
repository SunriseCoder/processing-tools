package files;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import filters.CustomFilenameFilter;
import helpers.FilesHelper;
import progress.ProgressPrinter;
import utils.FileUtils;

public class ConcatMTSFiles {

    public static void main(String[] args) throws IOException {
        System.out.println("Preparing video list for MTS-files concat...");

        File currentFolder = new File(".");

        FilenameFilter filter = new CustomFilenameFilter(false, "mts");
        String[] filenames = currentFolder.list(filter);

        if (filenames.length == 0) {
            System.out.println("MTS files not found");
            System.exit(-1);
        }

        // Filenames - temporary name and final name
        String firstFilename = filenames[0];
        String tmpFilename = FileUtils.getFileName(firstFilename) + "_concat.tmp";
        String resultFilename = FileUtils.getFileName(firstFilename) + "_concat.mts";

        // Checking files
        long totalFileSize = 0;
        List<File> files = new ArrayList<>();
        try (PrintWriter jointsPrintWriter = new PrintWriter("joints.txt")) {
            Duration totalDuration = Duration.ZERO;
            for (String filename : filenames) {
                File file = new File(filename);
                if (file.isDirectory()) {
                    System.out.println("Error: File is a directory");
                }
                if (!file.exists()) {
                    System.out.println("Error: File does not exists");
                }

                totalFileSize += file.length();
                files.add(file);

                // Joint timings
                Duration duration = getVideoDuration(filename);
                jointsPrintWriter.println(format(totalDuration) + " - " + filename);
                totalDuration = totalDuration.plus(duration);
                System.out.println("File: " + filename + ", duration: " + format(duration) + ", total: " + format(totalDuration));
            }
        }

        // Preparing progress printer
        ProgressPrinter progressPrinter = new ProgressPrinter();
        progressPrinter.reset(totalFileSize);

        // Real data copy
        try (RandomAccessFile outputFile = new RandomAccessFile(tmpFilename, "rw");) {
            outputFile.setLength(totalFileSize);
            FileChannel out = outputFile.getChannel();

            for (File file : files) {
                try (RandomAccessFile inputFile = new RandomAccessFile(file, "r")) {
                    FileChannel in = inputFile.getChannel();
                    long read;
                    int copyChunkSize = 1024 * 1024;
                    ByteBuffer buffer = ByteBuffer.allocate(copyChunkSize);
                    while ((read = in.read(buffer)) > 0) {
                        buffer.flip();
                        out.write(buffer);
                        progressPrinter.printProgressIncrease(read, false);
                        buffer = ByteBuffer.allocate(copyChunkSize);
                    }
                }
            }

            progressPrinter.printProgressFinished();
        }

        System.out.println("Renaming file \"" + tmpFilename + "\" to \"" + resultFilename + "\"...");
        Files.move(Paths.get(tmpFilename), Paths.get(resultFilename), StandardCopyOption.ATOMIC_MOVE);

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
