package core.dto;

import java.util.List;

public class Command {
    private String name;
    private List<Operation> operations;

    public String getName() {
        return name;
    }

    public List<Operation> getOperations() {
        return operations;
    }
}
