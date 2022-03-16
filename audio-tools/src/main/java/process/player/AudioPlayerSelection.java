package process.player;

public class AudioPlayerSelection {
    private Long start;
    private Long end;

    public AudioPlayerSelection() {
        // Default Constructor
    }

    public AudioPlayerSelection(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public Long getStart() {
        return start;
    }

    public Long getEnd() {
        return end;
    }

    public boolean isStartEmpty() {
        return start == null;
    }

    public boolean isStartNotEmpty() {
        return start != null;
    }

    public boolean isEndEmpty() {
        return end == null;
    }

    public boolean isEndNotEmpty() {
        return end != null;
    }

    public void reset() {
        start = null;
        end = null;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public void setEnd(Long end) {
        this.end = end;
    }
}
