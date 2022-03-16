package process.dto;

public class Candle {
    public int first;
    public int last;
    public int min;
    public int max;

    public Candle(int first, int last, int min, int max) {
        this.first = first;
        this.last = last;
        this.min = min;
        this.max = max;
    }

    @Override
    public String toString() {
        return "Candle [first=" + first + ", last=" + last + ", min=" + min + ", max=" + max + "]";
    }
}
