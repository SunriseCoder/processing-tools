package process;

import java.io.File;
import java.util.List;

public class ProcessRunnerToFile extends AbstractProcessRunner<File> {
    private File outputFile;
    private File errorFile;

    @Override
    public File getErrors() {
        return errorFile;
    }

    @Override
    public File getOutput() {
        return outputFile;
    }

    @Override
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
        }

        return -1;
    }

    public void setErrorFile(File errorFile) {
        this.errorFile = errorFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }
}
