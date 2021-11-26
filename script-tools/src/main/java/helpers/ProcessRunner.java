package helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class ProcessRunner {
    private StringBuilder errors;
    private StringBuilder output;

    public ProcessRunner() {
        errors = new StringBuilder();
        output = new StringBuilder();
    }

    public StringBuilder getErrors() {
        return errors;
    }

    public StringBuilder getOutput() {
        return output;
    }

    public void execute(List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            try (
                    BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    ) {
                String line = null;
                while (process.isAlive()) {
                    while ((line = errorReader.readLine()) != null) {
                        errors.append(line).append("\n");
                    }

                    while ((line = outputReader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
