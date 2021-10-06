package wrappers;

public class ThreadSafeBooleanWrapper {
    private volatile boolean value;

    public ThreadSafeBooleanWrapper() {
        // Default constructor
    }

    public ThreadSafeBooleanWrapper(boolean value) {
        this.value = value;
    }

    public synchronized boolean value() {
        return value;
    }

    public synchronized void setValue(boolean value) {
        this.value = value;
    }
}
