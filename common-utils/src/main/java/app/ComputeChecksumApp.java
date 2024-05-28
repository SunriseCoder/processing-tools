package app;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

import app.checksum.ChecksumComputer;

public class ComputeChecksumApp {

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        ChecksumComputer checksumComputer = new ChecksumComputer(Arrays.asList(args[0]));
        for (int i = 1; i < args.length; i++) {
            File file = new File(args[i]);
            Map<String, String> checksums = checksumComputer.computeChecksums(file);
            System.out.println("File: " + file.getAbsolutePath() + ", checksums: " + checksums);
        }
    }
}
