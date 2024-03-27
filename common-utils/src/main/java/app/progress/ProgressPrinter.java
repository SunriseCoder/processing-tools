package app.progress;

public abstract class ProgressPrinter {

    public ProgressPrinter() {
        super();
    }

    public abstract void startNewFile(String fileName, long fileSize);
    public abstract void addProgress(long progress);
    public abstract void finishCurrentFile();
}
