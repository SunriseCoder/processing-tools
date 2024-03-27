package config;

import java.util.HashMap;
import java.util.Map;

import config.exception.InvalidTypeException;
import config.exception.ParameterNotSetException;
import config.exception.UnknownParameterException;

public abstract class BaseConfig {
    private Map<String, Object> values = new HashMap<>();

    public boolean hasKey(Parameter parameter) {
        boolean hasKey = values.containsKey(parameter.getName());
        return hasKey;
    }

    public boolean hasValue(Parameter parameter) {
        boolean hasValue = values.containsValue(parameter.getName());
        return hasValue;
    }

    public String getStringValue(Parameter parameter) {
        Object value = values.get(parameter.getName());

        if (value == null) {
            throw new ParameterNotSetException("Parameter \"" + parameter.getName() + "\" was not set");
        }

        if (value instanceof String) {
            return (String) value;
        }

        throw new InvalidTypeException("Invalid value type - expected: " + String.class.getName()
                + ", actual: " + value.getClass().getName());
    }

    public Integer getIntegerValue(Parameter parameter) {
        Object value = values.get(parameter.getName());

        if (value == null) {
            throw new ParameterNotSetException("Parameter \"" + parameter.getName() + "\" was not set");
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                int integerValue = Integer.parseInt((String) value);
                values.put(parameter.getName(), integerValue);
                return integerValue;
            } catch (NumberFormatException e) {
                throw new InvalidTypeException("Provided value is not an Integer value: " + value);
            }
        }

        throw new InvalidTypeException("Invalid value type - expected: " + Integer.class.getName()
                + ", actual: " + value.getClass().getName());
    }

    public Long getLongValue(Parameter parameter) {
        Object value = values.get(parameter.getName());

        if (value == null) {
            throw new ParameterNotSetException("Parameter \"" + parameter.getName() + "\" was not set");
        }

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                long longValue = Long.parseLong((String) value);
                values.put(parameter.getName(), longValue);
                return longValue;
            } catch (NumberFormatException e) {
                throw new InvalidTypeException("Provided value is not a Long value: " + value);
            }
        }

        throw new InvalidTypeException("Invalid value type - expected: " + Long.class.getName()
                + ", actual: " + value.getClass().getName());
    }

    public void validate(Parameter[] parameters) {
        Map<String, Parameter> parametersMap = new HashMap<>();
        for (Parameter parameter : parameters) {
            parametersMap.put(parameter.getName(), parameter);

            if (parameter.isMandatory()) {
                if (!hasKey(parameter)) {
                    throw new ParameterNotSetException("Mandatory Parameter \"--" + parameter.getName() + "\" was not set");
                }
            }

            if (hasKey(parameter)) {
                try {
                    if (parameter.getType().equals(Integer.class)) {
                        getIntegerValue(parameter);
                    } else if (parameter.getType().equals(Long.class)) {
                        getLongValue(parameter);
                    } else if (parameter.getType().equals(String.class)) {
                        getStringValue(parameter);
                    }
                } catch (Exception e) {
                    throw e;
                }
            }
        }

        for (String key : values.keySet()) {
            if (!parametersMap.containsKey(key)) {
                throw new UnknownParameterException("Unknown parameter: " + key);
            }
        }
    }

    protected void setValue(String key, Object value) {
        values.put(key, value);
    }
}
