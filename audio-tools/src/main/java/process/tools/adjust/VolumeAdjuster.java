package process.tools.adjust;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import audio.api.FrameOutputStream;
import audio.wav.WaveInputStream;
import progress.ProgressPrinter;
import utils.MathUtils;

public class VolumeAdjuster {
    private static final int MIN_VALUE = -32768;
    private static final int MAX_VALUE = 32767;

    private File inputFile;
    private int inputChannel;
    private FrameOutputStream outputStream;
    private int outputChannel;
    private ProgressPrinter progressPrinter;

    public VolumeAdjuster(File inputFile, int inputChannel,
            FrameOutputStream outputStream, int outputChannel, ProgressPrinter progressPrinter) {
        this.inputFile = inputFile;
        this.inputChannel = inputChannel;
        this.outputStream = outputStream;
        this.outputChannel = outputChannel;
        this.progressPrinter = progressPrinter;
    }

    public void adjust(List<FrameGroup> positiveGroups, List<FrameGroup> negativeGroups)
            throws IOException, UnsupportedAudioFileException {
        try (WaveInputStream inputStream = WaveInputStream.create(inputFile, inputChannel);) {
            long totalFrames = inputStream.getFramesCount();
            progressPrinter.reset(totalFrames);

            Iterator<FrameGroup> positiveGroupsIterator = positiveGroups.iterator();
            Iterator<FrameGroup> negativeGroupsIterator = negativeGroups.iterator();
            FrameGroup currentPositiveGroup = null;
            FrameGroup currentNegativeGroup = null;
            for (long scanPos = 0; scanPos < totalFrames; scanPos++) {
                int value = inputStream.readFrame();
                if (value > 0) {
                    while (currentPositiveGroup == null || currentPositiveGroup.endPos < scanPos) {
                        if (positiveGroupsIterator.hasNext()) {
                            currentPositiveGroup = positiveGroupsIterator.next();
                        } else {
                            break;
                        }
                    }
                    if (currentPositiveGroup.startPos <= scanPos && scanPos <= currentPositiveGroup.endPos) {
                        value = MathUtils.roundToInt(value * currentPositiveGroup.factor);
                    }
                    if (value > MAX_VALUE) {
                        value = MAX_VALUE;
                    }
                }
                if (value < 0) {
                    while (currentNegativeGroup == null || currentNegativeGroup.endPos < scanPos) {
                        if (negativeGroupsIterator.hasNext()) {
                            currentNegativeGroup = negativeGroupsIterator.next();
                        } else {
                            break;
                        }
                    }
                    if (currentNegativeGroup.startPos <= scanPos && scanPos <= currentNegativeGroup.endPos) {
                        value = MathUtils.roundToInt(value * currentNegativeGroup.factor);
                    }
                    if (value < MIN_VALUE) {
                        value = MIN_VALUE;
                    }
                }
                outputStream.write(outputChannel, value);

                progressPrinter.printProgress(scanPos, false);
            }

            progressPrinter.printProgressFinished();
        }
    }
}
