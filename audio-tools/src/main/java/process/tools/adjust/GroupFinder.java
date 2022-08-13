package process.tools.adjust;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import audio.wav.WaveInputStream;
import progress.ProgressPrinter;

public class GroupFinder {
    private File inputFile;
    private int inputChannel;
    private ProgressPrinter progressPrinter;

    private List<FrameGroup> positiveGroups;
    private List<FrameGroup> negativeGroups;

    public GroupFinder(File inputFile, int inputChannel, ProgressPrinter progressPrinter) {
        this.inputFile = inputFile;
        this.inputChannel = inputChannel;
        this.progressPrinter = progressPrinter;
    }

    public void findAllGroups() throws IOException, UnsupportedAudioFileException {
        positiveGroups = new ArrayList<>();
        negativeGroups = new ArrayList<>();

        try (WaveInputStream inputStream = WaveInputStream.create(inputFile, inputChannel);) {
            long totalFrames = inputStream.getFramesCount();
            progressPrinter.reset(totalFrames);
            GroupTypes currentGroupType = GroupTypes.START;
            FrameGroup currentGroup = null;

            for (long scanPos = 0; scanPos < totalFrames; scanPos++) {
                int value = inputStream.readFrame();

                switch (currentGroupType) {
                case START:
                    if (value > 0) {
                        currentGroupType = GroupTypes.POSITIVE;
                    } else if (value < 0) {
                        currentGroupType = GroupTypes.NEGATIVE;
                    }
                    break;
                case POSITIVE:
                    if (value <= 0) {
                        // End of positive group
                        positiveGroups.add(currentGroup);
                    }
                    if (value < 0) {
                        currentGroupType = GroupTypes.NEGATIVE;
                        currentGroup = new FrameGroup(scanPos, scanPos - 1);
                    }
                    break;
                case NEGATIVE:
                    if (value >= 0) {
                        // End of negative group
                        negativeGroups.add(currentGroup);
                    }
                    if (value > 0) {
                        currentGroupType = GroupTypes.POSITIVE;
                        currentGroup = new FrameGroup(scanPos, scanPos - 1);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
                }

                if (currentGroup == null && value != 0) {
                    currentGroup = new FrameGroup(scanPos, scanPos - 1);
                }
                if (value != 0) {
                    currentGroup.addValue(scanPos, value);
                }

                progressPrinter.printProgress(scanPos, false);
            }

            progressPrinter.printProgressFinished();
        }
    }

    public List<FrameGroup> getPositiveGroups() {
        return positiveGroups;
    }

    public List<FrameGroup> getNegativeGroups() {
        return negativeGroups;
    }

    private enum GroupTypes { START, POSITIVE, NEGATIVE }
}
