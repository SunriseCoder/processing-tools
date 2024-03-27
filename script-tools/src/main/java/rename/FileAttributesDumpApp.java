package rename;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import app.utils.JSONUtils;

public class FileAttributesDumpApp {
    private static Map<String, Map<String, Object>> fileAttributes = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            printUsage();
            System.exit(-1);
        }

        File folder = new File(args[0]);

        File rootFolder = folder.getAbsoluteFile().getParentFile();
        scanFolderRecursively(rootFolder, folder.getAbsoluteFile());
        saveDump();

        System.out.println("Dump is done");
    }

    private static void scanFolderRecursively(File root, File folder) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanFolderRecursively(root, file);
                continue;
            }

            String relativeFileName = root.toPath().relativize(file.toPath()).toString();
            System.out.println("Processing file: " + relativeFileName);

            Map<String, Object> attributes = Files.readAttributes(file.toPath(), "*");
            fileAttributes.put(relativeFileName, attributes);
            attributes.entrySet().forEach(attribute -> System.out.println("\t" + attribute));
        }
    }

    private static void saveDump() throws IOException {
        JSONUtils.saveToDisk(fileAttributes, new File("files-dump.json"));
    }

    private static void printUsage() {
        System.out.println("Usage: " + FileAttributesDumpApp.class.getName() + " <folder>\n"
                + "\t where\n"
                + "\t\t <folder> is a path of the files for dump.\n");
    }
}
