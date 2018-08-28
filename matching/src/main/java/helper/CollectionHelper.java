package helper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CollectionHelper {

    public static boolean and(Collection<Boolean> booleans) {
        return booleans != null && !booleans.isEmpty() && !booleans.contains(Boolean.FALSE);
    }

    public static boolean or(Collection<Boolean> booleans) {
        return booleans != null && !booleans.isEmpty() && booleans.contains(Boolean.TRUE);
    }

    public static <T> List<T> flatMap(Collection<Collection<T>> collections) {
        return collections.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }
}
