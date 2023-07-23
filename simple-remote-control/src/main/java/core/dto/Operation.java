package core.dto;

import java.util.List;

public class Operation {
    private String name;
    private List<String> args;

    public String getName() {
        return name;
    }

    public List<String> getArgs() {
        return args;
    }
}
