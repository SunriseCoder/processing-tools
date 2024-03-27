package config;

public interface Parameter {
    /**
     * Parameter name
     *
     * @return
     */
    String getName();

    /**
     * Parameter type
     *
     * Used for casting and validation of values on the stage of parsing parameters
     *
     * @return Class of the value type
     */
    Class<?> getType();

    /**
     * Indicates is the parameter mandatory or not
     *
     * @return
     */
    boolean isMandatory();
}
