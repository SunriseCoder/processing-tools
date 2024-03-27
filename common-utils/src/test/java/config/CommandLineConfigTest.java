package config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import config.exception.InvalidTypeException;
import config.exception.UnknownParameterException;

public class CommandLineConfigTest {

    @Test
    public void keyAndValueTest() {
        String[] args = { "--1=3"};
        BaseConfig config = new CommandLineConfig(args);

        assertTrue(config.hasKey(Parameters.Digit1String));
        assertEquals("3", config.getStringValue(Parameters.Digit1String));
    }

    @Test
    public void keyAndEmptyValueTest() {
        String[] args = { "--1="};
        BaseConfig config = new CommandLineConfig(args);

        assertTrue(config.hasKey(Parameters.Digit1String));
        assertEquals("", config.getStringValue(Parameters.Digit1String));
    }

    @Test
    public void keyAndNoValueTest() {
        String[] args = { "--2" };
        BaseConfig config = new CommandLineConfig(args);

        assertTrue(config.hasKey(Parameters.Digit2Integer));
        assertFalse(config.hasValue(Parameters.Digit2Integer));
    }

    @Test
    public void stringValueTest() {
        String[] args = { "--1=s"};
        BaseConfig config = new CommandLineConfig(args);

        assertTrue(config.hasKey(Parameters.Digit1String));
        assertEquals("s", config.getStringValue(Parameters.Digit1String));
    }

    @Test
    public void integerValueTest() {
        String[] args = { "--2=5" };
        BaseConfig config = new CommandLineConfig(args);

        assertTrue(config.hasKey(Parameters.Digit2Integer));
        assertEquals((Integer) 5, config.getIntegerValue(Parameters.Digit2Integer));
    }

    @Test
    public void longValueTest() {
        String[] args = { "--3=153" };
        BaseConfig config = new CommandLineConfig(args);

        assertTrue(config.hasKey(Parameters.Digit3Long));
        assertEquals((Long) 153L, config.getLongValue(Parameters.Digit3Long));
    }

    @Test
    public void validateMandatoryAbsentTest() {
        String[] args = { "--1=s" };
        BaseConfig config = new CommandLineConfig(args);
        config.validate(Parameters.values());
    }

    @Test(expected = InvalidTypeException.class)
    public void validateWrongTypeTest() {
        String[] args = { "--1=s", "--2=s" };
        BaseConfig config = new CommandLineConfig(args);
        config.validate(Parameters.values());
    }

    @Test(expected = UnknownParameterException.class)
    public void validateUnknownParameterTest() {
        String[] args = { "--1=s", "--force" };
        BaseConfig config = new CommandLineConfig(args);
        config.validate(Parameters.values());
    }

    @Test
    public void validateSuccessTest() {
        String[] args = { "--1=s" };
        BaseConfig config = new CommandLineConfig(args);
        config.validate(Parameters.values());
    }
}
