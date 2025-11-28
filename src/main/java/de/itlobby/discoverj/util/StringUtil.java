package de.itlobby.discoverj.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class StringUtil {

    protected static final List<String> ARTISTS_SEPARATOR_KEYWORDS = Stream.of(
                    "&amp;",
                    "+",
                    "&",
                    "feat",
                    "ft",
                    "featuring",
                    "vs",
                    "versus"
            )
            .flatMap(entry -> mutate(entry).stream())
            .toList();
    private static final Logger log = LogManager.getLogger(StringUtil.class);

    private StringUtil() {
        // Static class
    }

    public static String encodeRfc3986(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().equals("");
    }

    public static String getStringSeq(String query, int start, int end) {
        return query.substring(start, end);
    }

    public static String removeBetween(String text, String start, String end) {
        int first = text.indexOf(start);
        int last = text.indexOf(end) + 1;
        int diff = last - first;

        if (diff > 0) {
            String seq = getStringSeq(text, first, last);
            text = text.replace(seq, "");
        }

        return text;
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        return getFileExtension(fileName);
    }

    public static String getFileExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        String extension = "";
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }

        return extension;
    }

    public static String removeBrackets(String query) {
        String normal1 = "(";
        String normal2 = ")";
        String edge1 = "[";
        String edge2 = "]";
        String swift1 = "{";
        String swift2 = "}";

        if (query.contains(normal1) && query.contains(normal2)) {
            query = removeBetween(query, normal1, normal2);
        }

        if (query.contains(edge1) && query.contains(edge2)) {
            query = removeBetween(query, edge1, edge2);
        }

        if (query.contains(swift1) && query.contains(swift2)) {
            query = removeBetween(query, swift1, swift2);
        }

        return query.trim().replace("  ", " ");
    }

    public static String removeKeyWords(String query) {
        for (String s : ARTISTS_SEPARATOR_KEYWORDS) {
            query = query.replace(" " + s + " ", " ");
        }
        query = query.replace(", ", " ");

        return query;
    }

    private static List<String> mutate(String s) {
        s = s.toLowerCase();
        ArrayList<String> mut = new ArrayList<>();
        mut.add(firstCharToUppercase(s) + ".");
        mut.add(firstCharToUppercase(s));
        mut.add(s.toUpperCase() + ".");
        mut.add(s.toUpperCase());
        mut.add(s + ".");
        mut.add(s);
        return mut;
    }

    public static String firstCharToUppercase(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static boolean isInteger(String stringInt) {
        try {
            Integer.parseInt(stringInt);
            return true;
        } catch (NumberFormatException e) {
            log.debug(e.getMessage(), e);
            return false;
        }
    }

    public static String sizeToHumanReadable(long byteLength) {
        double calc = byteLength;
        int unit = 0;

        while (calc > 1024) {
            calc = calc / 1024;
            unit++;
        }

        String unitString = switch (unit) {
            case 0 -> "Byte";
            case 1 -> "KB";
            case 2 -> "MB";
            case 3 -> "GB";
            case 4 -> "TB";
            default -> "";
        };

        return "%d %s".formatted(Math.round(calc), unitString);
    }
}
