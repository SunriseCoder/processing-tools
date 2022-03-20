package utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HttpUtils {

    public static String decodeURL(String url) throws UnsupportedEncodingException {
        String result = URLDecoder.decode(url, "UTF-8");
        return result;
    }

    public static String extractGetParameterValue(String parameters, String parameter) {
        String[] parameterParts = parameters.split("&");
        for (String parameterPart : parameterParts) {
            if (parameterPart.startsWith(parameter + "=")) {
                String value = parameterPart.substring(parameter.length() + 1);
                return value;
            }
        }
        return null;
    }
}
