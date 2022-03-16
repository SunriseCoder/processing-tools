package parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import parsers.dto.Mp4Chunk;
import utils.PrimitiveUtils;

public class Mp4Parser {
    private static final int CHUNK_HEADER_SIZE = 8;
    private static final int CHUNK_HEADER_SIZE_VALUE_POSITION = 0;
    private static final int CHUNK_HEADER_SIZE_VALUE_LENGTH = 4;
    private static final int CHUNK_HEADER_TYPE_VALUE_POSITION = 4;
    private static final int CHUNK_HEADER_TYPE_VALUE_LENGTH = 4;

    private List<Mp4Chunk> chunks;

    public void parse(File file) throws IOException {
        chunks = new ArrayList<Mp4Chunk>();

        try (FileInputStream is = new FileInputStream(file);) {
            long positionInFile = 0;
            byte[] buffer = new byte[8];
            while (is.available() > 0) {
                int read = is.read(buffer);
                if (read != 8) {
                    throw new IllegalStateException("Could not parse 8 bytes of the header of a chunk");
                }
                Mp4Chunk chunk = new Mp4Chunk();
                chunk.setAddressInFile(positionInFile);
                positionInFile += read;

                long chunkDataSize = PrimitiveUtils.bigEndianByteArrayToLong(buffer, CHUNK_HEADER_SIZE_VALUE_POSITION, CHUNK_HEADER_SIZE_VALUE_LENGTH);
                chunk.setDataSize(chunkDataSize);
                String chunkType = new String(buffer, CHUNK_HEADER_TYPE_VALUE_POSITION, CHUNK_HEADER_TYPE_VALUE_LENGTH);
                chunk.setType(chunkType);

                switch (chunkType) {
                case "ftyp":
                    // TODO Parse Sub-Chunks
                    break;
                case "mdat":
                    // TODO Parse Sub-Chunks
                    break;
                case "moov":
                    // TODO Parse Sub-Chunks
                    break;
                default:
                    is.skip(chunkDataSize - CHUNK_HEADER_SIZE);
                    positionInFile += chunkDataSize - CHUNK_HEADER_SIZE;
                }

                chunks.add(chunk);
            }
        }

        System.out.println("File parsed, found chunks: " + chunks.size());
        for (Mp4Chunk chunk : chunks) {
            System.out.println(chunk);
        }
    }
}
