package utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    public static String extractJsonSubstringFromString(String jsonStringWithJunk) {
        int startPos = -1, endPos = -1;
        boolean isInsideStringValue = false;
        List<CharAmountPair> openedBrackets = new ArrayList<>();

        int length = jsonStringWithJunk.length();
        for (int i = 0; i < length; i++) {
            char currentChar = jsonStringWithJunk.charAt(i);
            if (currentChar == '"' && jsonStringWithJunk.charAt(i - 1) != '\\') {
                isInsideStringValue = !isInsideStringValue;
            }
            if (isInsideStringValue) {
                continue;
            }
            switch (currentChar) {
            case '[':
                if (openedBrackets.size() == 0) {
                    openedBrackets.add(new CharAmountPair('[', 1));
                    if (startPos == -1) {
                        startPos = i;
                    }
                } else if (openedBrackets.get(openedBrackets.size() - 1).character != '[') {
                    openedBrackets.add(new CharAmountPair('[', 1));
                } else {
                    openedBrackets.get(openedBrackets.size() - 1).amount++;
                }
                break;
            case '{':
                if (openedBrackets.size() == 0) {
                    openedBrackets.add(new CharAmountPair('{', 1));
                    if (startPos == -1) {
                        startPos = i;
                    }
                } else if (openedBrackets.get(openedBrackets.size() - 1).character != '{') {
                    openedBrackets.add(new CharAmountPair('{', 1));
                } else {
                    openedBrackets.get(openedBrackets.size() - 1).amount++;
                }
                break;
            case ']':
                if (openedBrackets.size() == 0 || openedBrackets.get(openedBrackets.size() - 1).character != '[') {
                    throw new IllegalStateException("Json Format Error: " + jsonStringWithJunk);
                } else if (openedBrackets.get(openedBrackets.size() - 1).amount > 1) {
                    openedBrackets.get(openedBrackets.size() - 1).amount--;
                } else {
                    openedBrackets.remove(openedBrackets.size() - 1);
                    if (openedBrackets.size() == 0 && endPos == -1) {
                        endPos = i;
                    }
                }
                break;
            case '}':
                if (openedBrackets.size() == 0 || openedBrackets.get(openedBrackets.size() - 1).character != '{') {
                    throw new IllegalStateException("Json Format Error: " + jsonStringWithJunk);
                } else if (openedBrackets.get(openedBrackets.size() - 1).amount > 1) {
                    openedBrackets.get(openedBrackets.size() - 1).amount--;
                } else {
                    openedBrackets.remove(openedBrackets.size() - 1);
                    if (openedBrackets.size() == 0 && endPos == -1) {
                        endPos = i;
                    }
                }
                break;
            }
            if (startPos != -1 && endPos != -1 && openedBrackets.size() == 0) {
                break;
            }
        }

        if (startPos == -1 || endPos == -1 || startPos > endPos || openedBrackets.size() > 0) {
            throw new IllegalStateException("Json Format Error: " + jsonStringWithJunk);
        }

        String result = jsonStringWithJunk.substring(startPos, endPos + 1);
        return result;
    }

    private static class CharAmountPair {
        private char character;
        private int amount;

        public CharAmountPair(char character, int amount) {
            this.character = character;
            this.amount = amount;
        }
    }

    public static List<String> findAllEntriesRecursively(JsonNode json, String mustContainText) {
        List<String> paths = new ArrayList<>();
        findAllEntriesRecursively(json, mustContainText, paths, "/");
        return paths;
    }

    public static void findAllEntriesRecursively(JsonNode json, String mustContainText, List<String> paths, String currentPath) {
        if (json.isArray()) {
            for (int i = 0; i < json.size(); i++) {
                JsonNode node = json.get(i);
                findAllEntriesRecursively(node, mustContainText, paths, currentPath + "[" + i + "]/");
            }
        } else if (json.isObject()) {
            for (Iterator<String> iter = json.fieldNames(); iter.hasNext();) {
                String fieldName = iter.next();
                JsonNode fieldValue = json.get(fieldName);
                if (fieldValue.isArray() || fieldValue.isObject()) {
                    findAllEntriesRecursively(fieldValue, mustContainText, paths, currentPath + fieldName + "/");
                } else if (fieldValue.isTextual()) {
                    findAllEntriesRecursively(fieldValue, mustContainText, paths, currentPath + fieldName);
                }
            }
        } else if (json.isTextual() && json.asText().contains(mustContainText)) {
            paths.add(currentPath);
        }
    }
}
