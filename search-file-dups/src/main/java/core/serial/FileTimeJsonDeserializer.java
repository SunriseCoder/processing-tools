package core.serial;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class FileTimeJsonDeserializer extends StdDeserializer<FileTime> {
    private static final long serialVersionUID = 1032786875804745442L;
    private static final String PATTERN_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS";

    private DateTimeFormatter formatter;

    public FileTimeJsonDeserializer() {
        this(null);
    }

    public FileTimeJsonDeserializer(Class<FileTime> t) {
        super(t);
        formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                .withZone(ZoneId.systemDefault());
    }

    @Override
    public FileTime deserialize(JsonParser jsonparser, DeserializationContext context)
      throws IOException, JsonProcessingException {
        String stringValue = jsonparser.getText();
        TemporalAccessor temporalAccessor = formatter.parse(stringValue);
        Instant instant = Instant.from(temporalAccessor);
        FileTime fileTime = FileTime.from(instant);
        return fileTime;
    }
}
