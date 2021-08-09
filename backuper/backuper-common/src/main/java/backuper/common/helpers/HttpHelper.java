package backuper.common.helpers;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

@SuppressWarnings("restriction")
public class HttpHelper {

    public static Map<String, String> parsePostParams(HttpExchange exchange) throws IOException {
        Map<String, String> paramMap = new LinkedHashMap<>();

        String requestBody = StreamHelper.readFromInputStream(exchange.getRequestBody());
        String[] paramEntries = requestBody.split("&");
        Arrays.stream(paramEntries).forEach(p -> {
            String[] paramParts = p.split("=");
            paramMap.put(paramParts[0], paramParts[1]);
        });

        return paramMap;
    }

    public static boolean validateMethod(HttpExchange exchange, String methodName) throws IOException {
        if (!methodName.equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, null);
            return false;
        }

        return true;
    }

    public static boolean validateNonEmptyParams(Map<String, String> actualParams, String[] expectedParams, HttpExchange exchange) throws IOException {
        for (String expectedParam : expectedParams) {
            String actualParamValue = actualParams.get(expectedParam);

            if (actualParamValue == null) {
                String message = "Parameter \"" + expectedParam + "\" is missing";
                sendResponse(exchange, 422, message);
                return false;
            }

            if (actualParamValue.isEmpty()) {
                String message = "Parameter \"" + expectedParam + "\" is empty";
                sendResponse(exchange, 422, message);
                return false;
            }
        }

        return true;
    }

    public static void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        exchange.sendResponseHeaders(code, message == null ? 0 : message.length());
        if (message != null && !message.isEmpty()) {
            exchange.getResponseBody().write(message.getBytes());
        }
        exchange.close();
    }
}
