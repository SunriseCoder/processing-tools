package app.pattern.listener;

public interface ChangedListenable extends ChangedListener {
    void addChangedListener(ChangedListener listener);
    void removeChangedListener(ChangedListener listener);
}
