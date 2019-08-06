package filters;

public class FilenameFilterImages extends CustomFilenameFilter {
    private static String[] IMAGE_EXTENSIONS = { "bmp", "jpg", "png", "gif", "tif", "tiff" };

    public FilenameFilterImages() {
        super(IMAGE_EXTENSIONS);
    }
}
