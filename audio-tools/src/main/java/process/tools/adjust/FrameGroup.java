package process.tools.adjust;

class FrameGroup {
    long startPos;
    long endPos;
    long peakPosition;
    int peakValue;
    double factor;

    public FrameGroup(long startPos, long endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public void addValue(long position, int value) {
        endPos++;
        if ((value < 0 && value < peakValue) || (value > 0 && value > peakValue)) {
            peakValue = value;
            peakPosition = position;
        }
    }

    @Override
    public String toString() {
        return "FrameGroup[start=" + startPos + ", end=" + endPos + ", peak=" + peakValue + ", factor=" + factor + "]";
    }
}
