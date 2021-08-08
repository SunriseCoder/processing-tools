package backuper.client.benchmarks;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class ScanFoldersBenchmark {

    public static void main(String[] args) throws IOException {
        String pathString = "D:\\tmp\\big-folder";
        long start, duration = 0;

        System.out.println("Just a manual scan:");
        for (int i = 0; i < 3; i++) {
            start = System.currentTimeMillis();
            manualScan(Paths.get(pathString));
            duration = System.currentTimeMillis() - start;
            System.out.println("\tAttempt " + i + ": " + duration + " ms");
        }
        System.out.println();

        System.out.println("Manual scan, read all attributes at once:");
        for (int i = 0; i < 3; i++) {
            start = System.currentTimeMillis();
            manualScanGroupAttributes(Paths.get(pathString));
            duration = System.currentTimeMillis() - start;
            System.out.println("\tAttempt " + i + ": " + duration + " ms");
        }
        System.out.println();

        System.out.println("Optimized scan:");
        for (int i = 0; i < 3; i++) {
            start = System.currentTimeMillis();
            optimizedScan(Paths.get(pathString));
            duration = System.currentTimeMillis() - start;
            System.out.println("\tAttempt " + i + ": " + duration + " ms");
        }
        System.out.println();
    }

    private static void manualScan(Path folder) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path path : ds) {
                Files.size(path);
                Map<String, Object> attributes = Files.readAttributes(path, "lastModifiedTime");
                attributes.get("lastModifiedTime");

                if (Files.isDirectory(path)) {
                    manualScan(path);
                }
            }
        }
    }

    private static void manualScanGroupAttributes(Path folder) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path path : ds) {
                Files.size(path);
                Map<String, Object> attributes = Files.readAttributes(path, "lastModifiedTime,size");
                attributes.get("lastModifiedTime");
                attributes.get("size");

                if (Files.isDirectory(path)) {
                    manualScan(path);
                }
            }
        }
    }

    private static void optimizedScan(Path folder) throws IOException {
        HashMap <Path, BasicFileAttributes> attrs = new HashMap<>();
        BiPredicate<Path, BasicFileAttributes> predicate = (p, a) -> {
            return attrs.put(p, a) == null;
        };
        try (Stream<Path> stream = Files.find(folder, Integer.MAX_VALUE, predicate)) {
            stream.forEach(
                    p -> {
                        attrs.get(p).size();
                        attrs.get(p).lastModifiedTime();
                    }
            );
        }
    }
}
