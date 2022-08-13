package process.tools.adjust;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import audio.api.FrameOutputStream;
import audio.api.FrameStreamProcessor;
import audio.wav.WaveInputStream;
import progress.ProgressPrinter;
import utils.MathUtils;

public class FrameStreamAdjuster2 implements FrameStreamProcessor {
    private static final int MAX_FACTOR = 10;
    private static final int MIN_VALUE = -32768;
    private static final int MAX_VALUE = 32767;

    private File inputFile;
    private int inputChannel;
    private FrameOutputStream outputStream;
    private int outputChannel;
    private int sampleRate;

    private ProgressPrinter progressPrinter;

    private List<FrameGroup> positiveGroups;
    private List<FrameGroup> positiveLeaderGroups;
    private List<FrameGroup> negativeGroups;
    private List<FrameGroup> negativeLeaderGroups;

    public FrameStreamAdjuster2(File inputFile, int inputChannel, FrameOutputStream outputStream, int outputChannel)
            throws UnsupportedAudioFileException, IOException {
        this.inputFile = inputFile;
        this.inputChannel = inputChannel;
        this.outputStream = outputStream;
        this.outputChannel = outputChannel;

        WaveInputStream inputStream = WaveInputStream.create(inputFile);
        this.sampleRate = MathUtils.roundToInt(inputStream.getFormat().getSampleRate());
    }

    @Override
    public void setProgressPrinter(ProgressPrinter progressPrinter) {
        this.progressPrinter = progressPrinter;
    }

    @Override
    public void setPortionSize(int chunkSize) {
        // Nothing to do here
        // TODO Review the design to get rid of this method in the interface
        // and make the decision in each particular implementation
    }

    @Override
    public void prepareOperation() throws IOException, UnsupportedAudioFileException {
        // Nothing to do here
    }

    @Override
    public void process() throws IOException, UnsupportedAudioFileException {
        findAllGroups();
        extractLeaderGroups();
        assignGroupToLeaders();
        adjustVolume();
    }

    private void findAllGroups() throws UnsupportedAudioFileException, IOException {
        progressPrinter.println("\nAudio normalization:");
        progressPrinter.println("\tFinding groups:");
        GroupFinder groupFinder = new GroupFinder(inputFile, inputChannel, progressPrinter);
        groupFinder.findAllGroups();
        positiveGroups = groupFinder.getPositiveGroups();
        negativeGroups = groupFinder.getNegativeGroups();
    }

    private void extractLeaderGroups() throws UnsupportedAudioFileException, IOException {
        LeaderGroupExtractor extractor = new LeaderGroupExtractor(sampleRate, progressPrinter);
        progressPrinter.println("\n\tExtracting positive leader groups:");
        positiveLeaderGroups = extractor.extractLeaderGroups(positiveGroups);
        progressPrinter.println("\n\tExtracting negative leader groups:");
        negativeLeaderGroups = extractor.extractLeaderGroups(negativeGroups);
    }

    private void assignGroupToLeaders() {
        GroupsToLeadersAssigner assigner = new GroupsToLeadersAssigner(progressPrinter);
        progressPrinter.println("\n\tAssigning positive groups to leaders:");
        positiveLeaderGroups = assigner.assign(positiveGroups, positiveLeaderGroups);
        calculateFactors(positiveLeaderGroups);
        progressPrinter.println("\n\tAssigning negative groups to leaders:");
        negativeLeaderGroups = assigner.assign(negativeGroups, negativeLeaderGroups);
        calculateFactors(negativeLeaderGroups);
    }

    private void calculateFactors(List<FrameGroup> groups) {
        for (FrameGroup group : groups) {
            group.factor = group.peakValue > 0
                    ? 1.0 * MAX_VALUE / group.peakValue
                    : 1.0 * MIN_VALUE / group.peakValue;
            group.factor = Math.min(group.factor, MAX_FACTOR);
        }
    }

    private void adjustVolume() throws IOException, UnsupportedAudioFileException {
        progressPrinter.println("\n\tAdjusting volume according to the leader groups:");
        VolumeAdjuster adjuster = new VolumeAdjuster(inputFile, inputChannel, outputStream, outputChannel, progressPrinter);
        adjuster.adjust(positiveLeaderGroups, negativeLeaderGroups);
    }

    @Override
    public void close() {
        try {
            outputStream.close();
        } catch (IOException e) {
            // Just swallowing
        }
    }
}
