package process;

import java.util.List;

public abstract class AbstractProcessRunner<T> {
    public abstract T getErrors();
    public abstract T getOutput();
    public abstract int execute(List<String> command);
}
