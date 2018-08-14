package helper;

public class StringHelper {
    public static boolean isAbbreviated(int tokenLength, String name, Byte tokenOrder) {
        if(tokenLength == 1) return true;
        if(tokenLength == 2) {
            String[] splits = name.split("[^\\W']+");
            return splits.length > tokenOrder && splits[tokenOrder].startsWith(".");
        }
        return false;
    }
}
