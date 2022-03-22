package process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class ProcessRunnerToString extends AbstractProcessRunner<StringBuilder> {
    private StringBuilder errors;
    private StringBuilder output;

    public ProcessRunnerToString() {
        errors = new StringBuilder();
        output = new StringBuilder();
    }

    @Override
    public StringBuilder getErrors() {
        return errors;
    }

    @Override
    public StringBuilder getOutput() {
        return output;
    }

    @Override
    public int execute(List<String> command) {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            process = processBuilder.start();

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

                int result = process.exitValue();
                return result ;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
}
