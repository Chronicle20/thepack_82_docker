package net.sf.odinms.tools;

public class StringUtil {

    public static String getLeftPaddedStr(String in, char padchar, int length) {
        return String.valueOf(padchar).repeat(Math.max(0, length - in.length())) + in;
    }

    public static String getRightPaddedStr(String in, char padchar, int length) {
        return in + String.valueOf(padchar).repeat(Math.max(0, length - in.length()));
    }

    public static String joinStringFrom(String[] arr, int start) {
        return joinStringFrom(arr, start, " ");
    }

    public static String joinStringFrom(String[] arr, int start, String sep) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(sep);
            }
        }
        return builder.toString();
    }

    public static String makeEnumHumanReadable(String enumName) {
        StringBuilder builder = new StringBuilder(enumName.length() + 1);
        String[] words = enumName.split("_");
        for (String word : words) {
            if (word.length() <= 2) {
                builder.append(word); // assume that it's an abbrevation
            } else {
                builder.append(word.charAt(0));
                builder.append(word.substring(1).toLowerCase());
            }
            builder.append(' ');
        }
        return builder.substring(0, enumName.length());
    }

    public static int countCharacters(String str, char chr) {
        int ret = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == chr) {
                ret++;
            }
        }
        return ret;
    }
}
