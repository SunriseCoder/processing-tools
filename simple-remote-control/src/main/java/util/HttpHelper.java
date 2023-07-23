package util;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class HttpHelper {
    public static Map<String, String> parsePostParams(HttpExchange exchange) throws IOException {
        Map<String, String> paramMap = new LinkedHashMap<>();

        byte[] buffer = new byte[exchange.getRequestBody().available()];
        exchange.getRequestBody().read(buffer);
        String requestBody = new String(buffer);

        if (requestBody.isEmpty()) {
            return paramMap;
        }

        String[] paramEntries = requestBody.split("&");
        Arrays.stream(paramEntries).forEach(p -> {
            String[] paramParts = p.split("=");
            paramMap.put(paramParts[0], paramParts[1]);
        });

        return paramMap;
    }
}
