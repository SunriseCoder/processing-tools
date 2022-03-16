package utils;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

public class JSONUtils {

    public static JsonNode loadFromDisk(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        JsonNode result = mapper.readTree(file);
        return result;
    }

    public static <T> T loadFromDisk(File file, TypeReference<T> typeReference) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        T result = mapper.readValue(file, typeReference);
        return result;
    }

    public static void saveToDisk(Object object, File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, object);
    }

    public static String toJSON(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        mapper.setTimeZone(TimeZone.getDefault());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        String json = writer.writeValueAsString(object);
        return json;
    }

    public static JsonNode parseJSON(String jsonText) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        JsonNode node = mapper.readTree(jsonText);
        return node;
    }

    public static <T> T parseJSON(String text, TypeReference<T> typeReference) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        T result = mapper.readValue(text, typeReference);
        return result;
    }

    public static String trimAfterJsonEnds(String jsonStringWithJunk) {
        if (jsonStringWithJunk.charAt(0) != '{') {
            return null;
        }

        int openBraceCounter = 0;
        boolean isInsideString = false;
        for (int i = 0; i < jsonStringWithJunk.length(); i++) {
            char currentChar = jsonStringWithJunk.charAt(i);
            if (currentChar == '{') {
                openBraceCounter++;
            }
            if (!isInsideString && currentChar == '}') {
                openBraceCounter--;
            }
            if (!isInsideString && currentChar == '"') {
                isInsideString = true;
            }
            if (isInsideString && currentChar == '"' && jsonStringWithJunk.charAt(i - 1) != '\\') {
                isInsideString = false;
            }
            if (openBraceCounter == 0) {
                String result = jsonStringWithJunk.substring(0, i + 1);
                return result;
            }
        }

        return jsonStringWithJunk;
    }
}
