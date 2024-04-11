package app.core.comparator;

import java.util.Comparator;

import app.core.dto.FileMetadata;

public class ClosestToDestFolderComparator implements Comparator<FileMetadata> {
    private String destinationFolder;

    public ClosestToDestFolderComparator(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    @Override
    public int compare(FileMetadata o1, FileMetadata o2) {
        // Sorting objects by their proximity to the destination folder
        int o1match = matchFirstSymbols(o1.getAbsolutePath(), destinationFolder);
        int o2match = matchFirstSymbols(o2.getAbsolutePath(), destinationFolder);
        return o2match - o1match;
    }

    private int matchFirstSymbols(String string1, String string2) {
        char[] chars1 = string1.toCharArray();
        char[] chars2 = string2.toCharArray();
        int minSize = Math.min(chars1.length, chars2.length);

        for (int i = 0; i < minSize; i++) {
            if (chars1[i] != chars2[i]) {
                return i;
            }
        }

        return minSize;
    }
}
