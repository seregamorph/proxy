package com.seregamorph.proxy.escape;

public class EscapeUtils {
    private static final Escaper ESCAPER = new PercentEscaper("=?{}[]()\"\' :;,._/\\", false);

    private EscapeUtils() {
    }

    public static String escape(String str) {
        if (str == null) {
            return "null";
        }
        return ESCAPER.escape(str);
    }
}
