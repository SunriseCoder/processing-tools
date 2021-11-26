package video;

import java.io.File;
import java.util.List;

public class ProcessRunnerFileOutput {
    private File outputFile;
    private File errorFile;

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setErrorFile(File errorFile) {
        this.errorFile = errorFile;
    }

    public void execute(List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectOutput(outputFile);
            processBuilder.redirectError(errorFile);
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
