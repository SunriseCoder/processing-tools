package config;

public enum Parameters implements Parameter {
    Digit1String("1", String.class, true),
    Digit2Integer("2", Integer.class, false),
    Digit3Long("3", Long.class, false),
    Digit4Flag("4", Void.class, false);

    private String name;
    private Class<?> type;
    private boolean mandatory;

    Parameters(String name, Class<?> type, boolean mandatory) {
        this.name = name;
        this.type = type;
        this.mandatory = mandatory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }
}
