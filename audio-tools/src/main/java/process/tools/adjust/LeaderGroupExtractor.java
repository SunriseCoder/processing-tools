package process.tools.adjust;

import java.util.ArrayList;
import java.util.List;

import progress.ProgressPrinter;

public class LeaderGroupExtractor {
    private int sampleRate;

    private ProgressPrinter progressPrinter;

    public LeaderGroupExtractor(int sampleRate, ProgressPrinter progressPrinter) {
        this.sampleRate = sampleRate;
        this.progressPrinter = progressPrinter;
    }

    public List<FrameGroup> extractLeaderGroups(List<FrameGroup> groups) {
        List<FrameGroup> leaderGroups = new ArrayList<>();

        FrameGroup lastGroup = null;
        progressPrinter.reset(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            FrameGroup currentGroup = groups.get(i);
            // Checking that Last Group can't lead Current Group
            if (leaderGroups.size() == 0) {
                leaderGroups.add(currentGroup);
                lastGroup = currentGroup;
            } else {
                // If current group is a good candidate for a leadership
                if (canLeadGroup(lastGroup, currentGroup)) {
                    // Keep last group as current leader
                } else {
                    do {
                        lastGroup = leaderGroups.get(leaderGroups.size() - 1);
                        if (canLeadGroup(currentGroup, lastGroup)) {
                            leaderGroups.remove(leaderGroups.size() - 1);
                            lastGroup = currentGroup;
                        } else {
                            break;
                        }
                    } while (leaderGroups.size() > 0);
                    leaderGroups.add(currentGroup);
                }
            }
            progressPrinter.printProgress(i, false);
        }
        progressPrinter.printProgressFinished();

        return leaderGroups;
    }

    private boolean canLeadGroup(FrameGroup leader, FrameGroup group) {
        if (group.peakValue > leader.peakValue) {
            return false;
        }

        double rangePeakPositionDelta = 100.0 * Math.abs(leader.peakPosition - group.peakPosition) / sampleRate;
        double rangePeakValuePercentageDelta = 100.0 * Math.abs(leader.peakValue - group.peakValue) /
                Math.max(Math.abs(leader.peakValue), Math.abs(group.peakValue));
        double range = rangePeakValuePercentageDelta - Math.pow(rangePeakPositionDelta, 2);
        boolean result = range > 0;
        return result;
    }
}
