package image;

public class RecursivePhotoPreview {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1000; i++) {
            System.out.print(i + "\r");
            Thread.sleep(500);
        }
        /*if (args.length < 1) {
            printUsage();
            System.exit(-1);
        }

        String folderPath = args[0];
        File dataFile = FileUtils.createFile(folderPath, "image-data.json", true);
        if ()
        scanFolderRecursively();
        copyResourceFiles();*/
    }

    private static void scanFolderRecursively() {
        // TODO Auto-generated method stub

    }

    private static void printUsage() {
        System.out.println("Usage: " + RecursivePhotoPreview.class.getName() + " <path>");
    }

}
