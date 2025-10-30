package io.akeyless.cloudid.util;

/**
 * Tiny helpers to extract simple string fields from small JSON payloads without external deps.
 * This is not a full JSON parser; it handles flat objects with simple string values.
 */
public final class JsonExtractors {
    private JsonExtractors() {
    }

    public static String extractStringField(String json, String fieldName) {
        if (json == null || fieldName == null) {
            return null;
        }
        String needle = quote(fieldName) + ":";
        int idx = json.indexOf(needle);
        if (idx < 0) {
            return null;
        }
        // Move to value start
        int i = idx + needle.length();
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length()) {
            return null;
        }
        if (json.charAt(i) == '"') {
            // Quoted string
            i++;
            StringBuilder sb = new StringBuilder();
            boolean escaping = false;
            for (; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaping) {
                    // Handle basic escapes only
                    sb.append(c);
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } else {
            // Unquoted value (rare here), read until comma/brace
            int j = i;
            while (j < json.length()) {
                char c = json.charAt(j);
                if (c == ',' || c == '}' || c == '\n' || c == '\r') {
                    break;
                }
                j++;
            }
            String raw = json.substring(i, j).trim();
            if (raw.isEmpty()) {
                return null;
            }
            return raw;
        }
    }

    private static String quote(String s) {
        return '"' + s + '"';
    }
}


