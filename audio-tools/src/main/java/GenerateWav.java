import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import audio.wav.WaveOutputStream;

public class GenerateWav {

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        AudioFormat format = new AudioFormat(48000, 16, 1, true, true);
        // TODO Auto-generated method stub
        WaveOutputStream wos = new WaveOutputStream(new File("wave-line-zero.wav"), format);
        wos.writeHeader();

        int[] buffer = new int[65536];

        for (int i = 0; i < 65536; i++) {
            //int value = i + Short.MIN_VALUE;
            int value = 0;
            System.out.println("i: " + i + ", value: " + value);
            buffer[i] = value;
        }

        wos.write(0, buffer);
        wos.close();
    }
}
