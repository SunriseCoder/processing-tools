package filters;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.utils.FileUtils;

public class ExclusiveFilenameFilter implements FilenameFilter {
    private Set<String> exclusiveExtensions;

    public ExclusiveFilenameFilter(String... extensions) {
        List<String> extensionList = Arrays.asList(extensions);
        this.exclusiveExtensions = new HashSet<>(extensionList);
    }

    @Override
    public boolean accept(File dir, String name) {
        if (new File(dir, name).isDirectory()) {
            return false;
        }

        String extension = FileUtils.getFileExtension(name);
        extension = extension.toLowerCase();
        boolean result = !exclusiveExtensions.contains(extension);
        return result;
    }
}
