package process.tools.adjust;

import java.util.Iterator;
import java.util.List;

import progress.ProgressPrinter;
import utils.MathUtils;

public class LeaderHarmonizer {
    private static final double PROXIMITY_RANGE_BETWEEN_GROUPS_IN_SECONDS = 1;
    private static final int WEIGHT_OF_MINIMAL_FACTOR = 10;

    private ProgressPrinter progressPrinter;
    private int sampleRate;

    public void setProgressPrinter(ProgressPrinter progressPrinter) {
        this.progressPrinter = progressPrinter;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void harmonize(List<FrameGroup> positiveGroups, List<FrameGroup> negativeGroups) {
        if (positiveGroups.isEmpty() || negativeGroups.isEmpty()) {
            return;
        }

        Iterator<FrameGroup> positiveGroupsIterator = positiveGroups.iterator();
        Iterator<FrameGroup> negativeGroupsIterator = negativeGroups.iterator();

        if (progressPrinter != null) {
            progressPrinter.reset(Math.min(positiveGroups.size(), negativeGroups.size()));
        }
        Iterator<FrameGroup> progressIterator = positiveGroups.size() < negativeGroups.size() ? positiveGroupsIterator : negativeGroupsIterator;

        FrameGroup currentPositiveGroup = positiveGroupsIterator.next();
        FrameGroup currentNegativeGroup = negativeGroupsIterator.next();
        if (progressPrinter != null) {
            progressPrinter.printProgressIncrease(1, false);
        }

        int proximityInSamples = MathUtils.roundToInt(PROXIMITY_RANGE_BETWEEN_GROUPS_IN_SECONDS * sampleRate);
        do {
            // Checking proximity
            if (hasEnoughProximity(currentPositiveGroup, currentNegativeGroup, proximityInSamples)) {
                harmonizeGroups(currentPositiveGroup, currentNegativeGroup);
            }

            // If positive group is lagging, pulling its next element
            if (currentPositiveGroup.endPos < currentNegativeGroup.endPos) {
                if (positiveGroupsIterator.hasNext()) {
                    currentPositiveGroup = positiveGroupsIterator.next();
                    if (progressPrinter != null && positiveGroupsIterator == progressIterator) {
                        progressPrinter.printProgressIncrease(1, false);
                    }
                } else {
                    break;
                }
            // If negative group is lagging, pulling its next element
            } else if (currentPositiveGroup.endPos > currentNegativeGroup.endPos) {
                if (negativeGroupsIterator.hasNext()) {
                    currentNegativeGroup = negativeGroupsIterator.next();
                    if (progressPrinter != null && negativeGroupsIterator == progressIterator) {
                        progressPrinter.printProgressIncrease(1, false);
                    }
                } else {
                    break;
                }
            // If both group finished simultaneously, pulling their both's next elements
            } else {
                if (positiveGroupsIterator.hasNext() && negativeGroupsIterator.hasNext()) {
                    currentPositiveGroup = positiveGroupsIterator.next();
                    currentNegativeGroup = negativeGroupsIterator.next();
                    if (progressPrinter != null) {
                        progressPrinter.printProgressIncrease(1, false);
                    }
                } else {
                    break;
                }
            }
        } while (true);

        if (progressPrinter != null) {
            progressPrinter.printProgressFinished();
        }
    }

    public boolean hasEnoughProximity(FrameGroup group1, FrameGroup group2, long proximity) {
        boolean hasIntersection = group1.endPos >= group2.startPos - proximity && group2.endPos >= group1.startPos - proximity;
        return hasIntersection;
    }

    private void harmonizeGroups(FrameGroup group1, FrameGroup group2) {
        if (group1.factor > group2.factor) {
            group1.factor = (group1.factor + WEIGHT_OF_MINIMAL_FACTOR * group2.factor) / (WEIGHT_OF_MINIMAL_FACTOR + 1);
        } else {
            group2.factor = (WEIGHT_OF_MINIMAL_FACTOR * group1.factor + group2.factor) / (WEIGHT_OF_MINIMAL_FACTOR + 1);
        }
    }
}
