package utils;

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

    public int execute(List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectOutput(outputFile);
            processBuilder.redirectError(errorFile);
            Process process = processBuilder.start();
            process.waitFor();
            int result = process.exitValue();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
