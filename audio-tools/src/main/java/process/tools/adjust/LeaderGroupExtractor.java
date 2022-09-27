package process.tools.adjust;

import java.util.ArrayList;
import java.util.List;

import progress.ProgressPrinter;

public class LeaderGroupExtractor {
    /**
     * This is the factor how fast the leadership influence fading out
     * 50 = 100% per 2 seconds
     * 100 = 100% per second
     * 200 = 100% per 0.5 second
     */
    private static final int LEADERSHIP_INFLUENCE_ANGLE = 50;

    private int sampleRate;

    private ProgressPrinter progressPrinter;

    public LeaderGroupExtractor(int sampleRate, ProgressPrinter progressPrinter) {
        this.sampleRate = sampleRate;
        this.progressPrinter = progressPrinter;
    }

    public List<FrameGroup> extractLeaderGroups(List<FrameGroup> groups) {
        List<FrameGroup> leaderGroups = new ArrayList<>();

        FrameGroup currentLeaderGroup = null;
        progressPrinter.reset(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            FrameGroup newGroup = groups.get(i);
            if (leaderGroups.size() == 0) {
                leaderGroups.add(newGroup);
                currentLeaderGroup = newGroup;
            } else {
                // If current Leader group is keeping leadership
                if (canLeadGroup(currentLeaderGroup, newGroup)) {
                    // Keep leader group as leader
                } else {
                    do {
                        // Removing all Leader groups that could be lead by new Group
                        currentLeaderGroup = leaderGroups.get(leaderGroups.size() - 1);
                        if (canLeadGroup(newGroup, currentLeaderGroup)) {
                            leaderGroups.remove(leaderGroups.size() - 1);
                        } else {
                            break;
                        }
                    } while (leaderGroups.size() > 0);
                    leaderGroups.add(newGroup);
                    currentLeaderGroup = newGroup;
                }
            }
            progressPrinter.printProgress(i, false);
        }
        progressPrinter.printProgressFinished();

        return leaderGroups;
    }

    private boolean canLeadGroup(FrameGroup leaderCandidate, FrameGroup group) {
        if (Math.abs(group.peakValue) > Math.abs(leaderCandidate.peakValue)) {
            return false;
        }

        double actualRangeBetweenPeakPositionsInSeconds = 1.0 * Math.abs(leaderCandidate.peakPosition - group.peakPosition) / sampleRate;
        double actualRangeBetweenPeakValuesInPercents = 100.0 * Math.abs(leaderCandidate.peakValue - group.peakValue) /
                Math.max(Math.abs(leaderCandidate.peakValue), Math.abs(group.peakValue));
        double expectedRangeBetweenPeakPositions = actualRangeBetweenPeakValuesInPercents / LEADERSHIP_INFLUENCE_ANGLE;
        boolean canLead = actualRangeBetweenPeakPositionsInSeconds <= expectedRangeBetweenPeakPositions;
        return canLead;
    }
}
