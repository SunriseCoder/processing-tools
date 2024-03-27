package app.utils;

import java.lang.reflect.Field;
import java.util.Collection;

public class CloneUtils {

    public static Object clone(Object source) {
        Object clone;
        try {
            clone = source.getClass().newInstance();

            for (Field field : clone.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                Object sourceValue = field.get(source);
                Object cloneValue = null;

                if (sourceValue instanceof Collection) {
                    cloneValue = cloneCollection((Collection<?>) field.get(source));
                } else if (fieldType.isArray()) {
                    // TODO Implement
                    throw new UnsupportedOperationException("Not yet implemented");
                } else if (fieldType.isPrimitive()) {
                    cloneValue = field.get(source);
                } else {
                    cloneValue = clone(sourceValue);
                }

                field.set(clone, cloneValue);

                System.out.println(field.getName() + ": " + field.getType());
            }
        } catch (Exception e) {
            throw new RuntimeException("Clone Exception" , e);
        }

        return clone;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Collection<?> cloneCollection(Collection<?> source) throws Exception {
        Collection collectionClone = source.getClass().newInstance();
        source.forEach(element -> {
            Object elementClone = clone(element);
            collectionClone.add(elementClone);
        });
        return collectionClone;
    }
}
