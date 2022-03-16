package apps;

import java.io.File;

import parsers.Mp4Parser;

public class Mp4Experiments {

    public static void main(String[] args) throws Exception {
        File file = new File("data\\1.mp4");
        Mp4Parser parser = new Mp4Parser();
        parser.parse(file);
    }
}
