package backuper.client.operations;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class OperationsComparator implements Comparator<Operation> {
    private Map<Class<?>, Strategy> classPriority;

    public OperationsComparator() {
        classPriority = new HashMap<>();

        int proirity = 0;

        classPriority.put(DeleteFileOperation.class, new Strategy(proirity++, true));
        classPriority.put(DeleteFolderOperation.class, new Strategy(proirity++, false));
        classPriority.put(CreateFolderOperation.class, new Strategy(proirity++, true));
        classPriority.put(CopyFileOperation.class, new Strategy(proirity++, true));
    }

    @Override
    public int compare(Operation o1, Operation o2) {
        if (o1.getClass().equals(o2.getClass())) {
            Strategy strategy = classPriority.get(o1.getClass());
            return strategy.pathAscending ? o1.getRelativePath().compareTo(o2.getRelativePath()) : o2.getRelativePath().compareTo(o1.getRelativePath());
        } else {
            return classPriority.get(o1.getClass()).classPriority - classPriority.get(o2.getClass()).classPriority;
        }
    }

    private static class Strategy {
        private int classPriority;
        private boolean pathAscending;

        public Strategy(int classPriority, boolean pathAscending) {
            this.classPriority = classPriority;
            this.pathAscending = pathAscending;
        }
    }
}
