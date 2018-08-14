package helper;

import java.util.Collection;

public class CollectionHelper {

    public static boolean and(Collection<Boolean> booleans) {
        return booleans != null && !booleans.isEmpty() && !booleans.contains(Boolean.FALSE);
    }

    public static boolean or(Collection<Boolean> booleans) {
        return booleans != null && !booleans.isEmpty() && booleans.contains(Boolean.TRUE);
    }
}
