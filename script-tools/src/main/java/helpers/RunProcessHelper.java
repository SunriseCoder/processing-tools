package helpers;

import java.util.List;

public class RunProcessHelper {

    public static String FinishAndGetOutput(List<String> command) {
        try {
            ProcessRunner runner = new ProcessRunner();
            runner.execute(command);
            String output = runner.getOutput().toString();
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
