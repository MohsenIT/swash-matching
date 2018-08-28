package helper;

public class StringHelper {
    public static boolean isAbbreviated(int tokenLength) {
        return tokenLength == 1;
    }

    public static boolean isBeforeDot(int tokenLength, String name, Byte tokenOrder) {
        String[] splits = name.split("[^\\W']+");
        return splits.length > tokenOrder && splits[tokenOrder].startsWith(".");
    }
}
