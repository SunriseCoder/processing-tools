package helpers;

import java.util.Arrays;
import java.util.List;

public class FilesHelper {

    public static double getVideoDuration(String filename) {
        // TODO Auto-generated method stub
        List<String> command = Arrays.asList("ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=duration",
                "-of", "csv=p=0", filename);
        String processOutput = RunProcessHelper.FinishAndGetOutput(command);
        String stringValue = processOutput.split("\n")[0];
        double result = Double.parseDouble(stringValue);
        return result;
    }
}
