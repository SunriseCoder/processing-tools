package backuper.common.helpers;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler.ResponseTrigger;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;

public class HttpHelper {

    public static Map<String, String> parsePostParams(String requestBody) throws IOException {
        Map<String, String> paramMap = new LinkedHashMap<>();

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

    public static boolean validateRequestMethod(HttpRequest request, String expectedMethod,
            ResponseTrigger responseTrigger, HttpContext context) throws HttpException, IOException {

        if (!request.getMethod().equals(expectedMethod)) {
            String errorMessage = "Unsupported Request Method " + request.getMethod() + ", expected Method: " + expectedMethod;
            sendHttpResponse(HttpStatus.SC_METHOD_NOT_ALLOWED, errorMessage, ContentType.TEXT_HTML, responseTrigger, context);
            return false;
        }

        return true;
    }

    public static boolean validateNonEmptyParams(Map<String, String> actualParams, String[] expectedParams,
            ResponseTrigger responseTrigger, HttpContext context) throws HttpException, IOException {

        for (String expectedParam : expectedParams) {
            String actualParamValue = actualParams.get(expectedParam);

            if (actualParamValue == null) {
                String message = "Parameter \"" + expectedParam + "\" is missing";
                sendHttpResponse(422, message, ContentType.TEXT_HTML, responseTrigger, context);
                return false;
            }

            if (actualParamValue.isEmpty()) {
                String message = "Parameter \"" + expectedParam + "\" is empty";
                sendHttpResponse(422, message, ContentType.TEXT_HTML, responseTrigger, context);
                return false;
            }
        }

        return true;
    }

    public static void sendHttpResponse(int responseCode, String message, ContentType contentType,
            ResponseTrigger responseTrigger, HttpContext context) throws HttpException, IOException {

        System.out.println("Sending response: " + responseCode + " " + message.substring(0, Math.min(100, message.length())));
        responseTrigger.submitResponse(AsyncResponseBuilder.create(responseCode).setEntity(message, contentType).build(), context);
    }

    public static Response sendPostRequest(String requestUrl, List<NameValuePair> postData) throws IOException, HttpException {
        CloseableHttpResponse apacheResponse = (CloseableHttpResponse) Request.post(requestUrl).bodyForm(postData).execute().returnResponse();
        Response response = new Response(apacheResponse.getCode(), EntityUtils.toByteArray(apacheResponse.getEntity()));
        return response;
    }

    public static class Response {
        private int code;
        private byte[] data;

        public Response(int code, byte[] data) {
            this.code = code;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public byte[] getData() {
            return data;
        }
    }
}
