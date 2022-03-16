package filters;

public class FilenameFilterVideos extends CustomFilenameFilter {
    private static String[] VIDEO_EXTENSIONS = { "avi", "mkv", "mov", "mp4", "mts", "wmv" };

    public FilenameFilterVideos(boolean includeFolders) {
        super(includeFolders, VIDEO_EXTENSIONS);
    }
}
