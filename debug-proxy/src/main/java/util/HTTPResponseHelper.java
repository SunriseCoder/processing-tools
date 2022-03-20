package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import adaptors.ByteArray;
import dto.HTTPRequest;
import dto.HTTPResponse;

public class HTTPResponseHelper {

    public static HTTPResponse forward(HTTPRequest clientRequest, InputStream inputStream, List<OutputStream> outputStreams) throws IOException {
        HTTPResponse response = new HTTPResponse();
        State state = State.Begin;
        int lineNumber = 0;
        int totalReadBytes = 0;

        ByteArray line = new ByteArray();
        byte[] oneByte = new byte[1];
        byte[] lineEnd = new byte[] {13, 10};
        int contentLength;
        while (state != State.Done) {
            switch (state) {
            case Begin:
                state = State.ReadNextBytes;
                break;
            case ReadNextBytes:
                //System.out.println("avail: " + inputStream.available());
                int readBytes = inputStream.read(oneByte);
                while (readBytes < 1) {
                    ThreadHelper.sleep(1);
                    readBytes = inputStream.read(oneByte);
                }

                totalReadBytes += readBytes;

                line.append(oneByte, 0, readBytes);

                if (line.endsWith(lineEnd)) {
                    state = State.ProcessHeaderLine;
                }
                break;
            case ProcessHeaderLine:
                String stringLine = line.createString("UTF-8");
                String stringLineLowerCase = stringLine.toLowerCase();

                //System.out.println("Line: " + stringLine);

                if (lineNumber == 0) {
                    response.setStatusLine(stringLine.replaceAll("(\\r|\\n)", "").trim());
                }
                if (line.size() == 2) {
                    //System.out.println("Boo!" + totalReadBytes);
                    // Processing Empty Line between Request Header and Request Body
                    if (response.getContentLength() > -1) {
                        state = State.ProcessRequestBodyByContentLength;
                    } else if (response.isTransferByChunks() && !"HEAD".equals(clientRequest.getMethod())) {
                        state = State.ProcessRequestBodyByChunks;
                    } else {
                        state = State.Done;
                    }

                    break;
                } else if (stringLineLowerCase.startsWith("transfer-encoding:") && stringLineLowerCase.substring(18).trim().equals("chunked")) {
                    response.setTransferByChunks(true);
                } else if (stringLineLowerCase.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(stringLineLowerCase.substring(15).trim());
                    response.setContentLength(contentLength);
                }

                response.addHeader(stringLine);
                line.clear();
                lineNumber++;
                state = State.ReadNextBytes;
                break;
            case ProcessRequestBodyByContentLength:
                contentLength = response.getContentLength();
                byte[] body = readBytes(inputStream, contentLength);
                response.setBody(body);
                state = State.Done;
                break;
            case ProcessRequestBodyByChunks:
                ByteArray responseBody = new ByteArray();
                int chunkSize;
                //System.out.print("Chunk sizes: ");
                do {
                    String lineString = readLine(inputStream);
                    responseBody.append(lineString.getBytes()).append(lineEnd);
                    chunkSize = Integer.parseInt(lineString, 16);
                    //System.out.print(chunkSize + ", ");
                    byte[] chunk = readBytes(inputStream, chunkSize + 2);
                    responseBody.append(chunk);
                } while (chunkSize > 0);
                //System.out.println("Done");
                response.setBody(responseBody.createBytes());
                state = State.Done;
                break;
            default:
                throw new RuntimeException("Unsupported Request Copy state: " + state);
            }
        }

        // Forwarding Server Response to Client
        byte[] rawResponse = response.generateHttpResponseAsBytes();
        for (OutputStream outputStream : outputStreams) {
            outputStream.write(rawResponse);
            outputStream.flush();
        }

        return response;
    }

    private static byte[] readBytes(InputStream inputStream, int contentLength) throws IOException {
        byte[] buffer = new byte[contentLength];
        int bodyTotalRead = 0;
        while (bodyTotalRead < contentLength) {
            int read = inputStream.read(buffer, bodyTotalRead, contentLength - bodyTotalRead);
            if (read < 1) {
                ThreadHelper.sleep(10);
            }
            bodyTotalRead += read;
        }
        return buffer;
    }

    private static String readLine(InputStream inputStream) throws IOException {
        ByteArray byteArray = new ByteArray();
        byte[] oneByte = new byte[1];
        byte[] lineEnd = new byte[] {13, 10};
        while (!byteArray.endsWith(lineEnd)) {
            while (inputStream.read(oneByte) < 1) {
                ThreadHelper.sleep(10);
            }
            byteArray.append(oneByte);
            //System.out.println(byteArray.size());
        }
        String line = byteArray.createString("UTF-8");
        //System.out.println(byteArray.size());
        line = line.replaceAll("(\\r|\\n)", "");
        return line;
    }

    private static enum State {
        Begin, Done, ReadNextBytes, ProcessHeaderLine, ProcessRequestBodyByChunks, ProcessRequestBodyByContentLength
    }
}
