package core.serial;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class FileTimeJsonSerializer extends StdSerializer<FileTime> {
    private static final long serialVersionUID = 1032786875804745442L;
    private static final String PATTERN_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    private DateTimeFormatter formatter;

    public FileTimeJsonSerializer() {
        this(null);
    }

    public FileTimeJsonSerializer(Class<FileTime> t) {
        super(t);
        formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                .withZone(ZoneId.systemDefault());
    }

    @Override
    public void serialize(FileTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Instant instant = value.toInstant();
        String stringValue = formatter.format(instant);
        gen.writeString(stringValue);
    }
}
