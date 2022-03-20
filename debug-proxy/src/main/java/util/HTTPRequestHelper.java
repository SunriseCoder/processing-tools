package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adaptors.ByteArray;
import dto.HTTPRequest;

public class HTTPRequestHelper {
    private static final Pattern REQUEST_LINE_REGEX = Pattern
            .compile("^([A-Z]+)\\shttp(s?)://([0-9a-z\\.-]+):?([0-9]*)(/?\\S*)\\s(\\S*)$");

    public static HTTPRequest fetch(InputStream inputStream) throws IOException {
        HTTPRequest request = new HTTPRequest();
        State state = State.Begin;
        int lineNumber = 0;

        ByteArray line = new ByteArray();
        byte[] oneByte = new byte[1];
        byte[] lineEnd = new byte[] {13, 10};
        while (state != State.Done) {
            switch (state) {
            case Begin:
                state = State.ReadNextBytes;
                break;
            case ReadNextBytes:
                int readBytes = inputStream.read(oneByte);
                while (readBytes < 1) {
                    ThreadHelper.sleep(1);
                    readBytes = inputStream.read(oneByte);
                }

                line.append(oneByte, 0, readBytes);

                if (line.endsWith(lineEnd)) {
                    state = State.ProcessHeaderLine;
                }
                break;
            case ProcessHeaderLine:
                String stringLine = line.createString("UTF-8");
                String stringLineLowerCase = stringLine.toLowerCase();

                if (lineNumber == 0) {
                    // Process Request Line
                    parseRequestLine(stringLine, request);
                    line.clear();
                    state = State.ReadNextBytes;
                    lineNumber++;
                    break;
                } else if (line.size() == 2) {
                    // Processing Empty Line between Request Header and Request Body
                    // TODO Check if need to process Request Body or Done
                    /*if (x) {
                        state = State.ProcessRequestBody;
                    } else {
                        state = State.Done;
                    }*/
                    state = State.Done;

                    break;
                } else if (stringLineLowerCase.startsWith("accept-encoding:")) {
                    // Preventing pages from being compressed by requesting plain text
                    stringLine = "Accept-Encoding: deflate\r\n";
                }

                // TODO Parse line if needed
                request.addHeader(stringLine);
                line.clear();
                lineNumber++;
                state = State.ReadNextBytes;
                break;
            case ProcessRequestBody:
                // TODO parse the Request Body
                state = State.Done;
                break;
            default:
                throw new RuntimeException("Unsupported Request Copy state: " + state);
            }
        }

        return request;
    }

    private static void parseRequestLine(String requestLine, HTTPRequest request) {
        requestLine = requestLine.replaceAll("(\\r|\\n)", "");
        Matcher matcher = REQUEST_LINE_REGEX.matcher(requestLine);
        if (!matcher.matches() || matcher.groupCount() < 6) {
            throw new RuntimeException("Could not parse Client Request Line Header: " + requestLine);
        }

        request.setMethod(matcher.group(1));
        request.setSSL("s".equals(matcher.group(2)));
        request.setHost(matcher.group(3));
        int port;
        if (!matcher.group(4).isEmpty()) {
            // Port from URL
            port = Integer.parseInt(matcher.group(4));
        } else if (matcher.group(2).isEmpty()) {
            // Default port
            port = 80;
        } else {
            // Default SSL port
            port = 443;
        }
        request.setPort(port);
        request.setPath(matcher.group(5));
        request.setProtocol(matcher.group(6));
    }

    public static void send(HTTPRequest request, List<OutputStream> outputStreams) throws IOException {
        byte[] rawRequest = request.generateHttpRequestAsBytes();
        for (OutputStream outputStream : outputStreams) {
            outputStream.write(rawRequest);
            outputStream.flush();
        }
    }

    private static enum State {
        Begin, Done, ReadNextBytes, ProcessHeaderLine, ProcessRequestBody
    }
}
