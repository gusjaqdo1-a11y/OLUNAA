package com.olunaa.app;

import java.util.List;

public final class TextUtils {
    private TextUtils() {}
    public static String join(String delimiter, List<?> values) {
        StringBuilder out = new StringBuilder();
        if (values == null) return "";
        for (Object value : values) {
            if (out.length() > 0) out.append(delimiter);
            out.append(String.valueOf(value));
        }
        return out.toString();
    }
}
