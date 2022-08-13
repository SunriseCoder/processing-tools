package process.tools.adjust;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import progress.ProgressPrinter;

public class GroupsToLeadersAssigner {
    private ProgressPrinter progressPrinter;

    public GroupsToLeadersAssigner(ProgressPrinter progressPrinter) {
        this.progressPrinter = progressPrinter;
    }

    public List<FrameGroup> assign(List<FrameGroup> groups, List<FrameGroup> leaderGroups) {
        if (leaderGroups.size() == 0 && groups.size() == 0) {
            return new ArrayList<>();
        }

        List<FrameGroup> result = new ArrayList<>();

        if (leaderGroups.size() == 1) {
            FrameGroup group = new FrameGroup(groups.get(0).startPos, groups.get(groups.size() - 1).endPos);
            FrameGroup leaderGroup = leaderGroups.get(0);
            group.peakPosition = leaderGroup.peakPosition;
            group.peakValue = leaderGroup.peakValue;
            result.add(group);
            return result;
        }

        progressPrinter.reset(groups.size());
        Iterator<FrameGroup> leaderGroupsIterator = leaderGroups.iterator();
        Iterator<FrameGroup> groupIterator = groups.iterator();
        FrameGroup leftLeader = cloneGroup(leaderGroupsIterator.next());
        result.add(leftLeader);
        FrameGroup group = groupIterator.next();
        do {
            if (group.endPos <= leftLeader.endPos) {
                // First assigning all the Groups to Left Leader, which are located at the beginning of the file
                consumeGroup(leftLeader, group);
                if (groupIterator.hasNext()) {
                    group = groupIterator.next();
                    progressPrinter.printProgressIncrease(1, false);
                } else {
                    progressPrinter.printProgressFinished();
                    return result;
                }
            } else {
                break;
            }
        } while (true);

        FrameGroup rightLeader = cloneGroup(leaderGroupsIterator.next());
        result.add(rightLeader);
        do {
            if (group.endPos <= rightLeader.endPos) {
                // Consume and next group
                FrameGroup consumer =
                        group.peakPosition - leftLeader.peakPosition <= rightLeader.peakPosition - group.peakPosition
                        ? leftLeader : rightLeader;
                consumeGroup(consumer, group);
                if (groupIterator.hasNext()) {
                    group = groupIterator.next();
                    progressPrinter.printProgressIncrease(1, false);
                } else {
                    progressPrinter.printProgressFinished();
                    return result;
                }
            } else {
                // Shift leader groups
                leftLeader = rightLeader;
                if (leaderGroupsIterator.hasNext()) {
                    rightLeader = cloneGroup(leaderGroupsIterator.next());
                    result.add(rightLeader);
                } else {
                    consumeGroup(leftLeader, group);
                    while (groupIterator.hasNext()) {
                        group = groupIterator.next();
                        consumeGroup(leftLeader, group);
                        progressPrinter.printProgressIncrease(1, false);
                    }
                    progressPrinter.printProgressFinished();
                    return result;
                }
            }
        } while (true);
    }

    private FrameGroup cloneGroup(FrameGroup group) {
        FrameGroup clonedGroup = new FrameGroup(group.startPos, group.endPos);
        clonedGroup.peakPosition = group.peakPosition;
        clonedGroup.peakValue = group.peakValue;
        clonedGroup.factor = group.factor;
        return clonedGroup;
    }

    private void consumeGroup(FrameGroup leader, FrameGroup group) {
        if (group.startPos < leader.startPos) {
            leader.startPos = group.startPos;
        }
        if (group.endPos > leader.endPos) {
            leader.endPos = group.endPos;
        }
        if ((group.peakValue > 0 && group.peakValue > leader.peakValue)
                || (group.peakValue < 0 && group.peakValue < leader.peakValue)) {
            leader.peakValue = group.peakValue;
            leader.peakPosition = group.peakPosition;
        }
    }
}
