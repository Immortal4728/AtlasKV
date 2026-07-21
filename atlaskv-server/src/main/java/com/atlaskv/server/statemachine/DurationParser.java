package com.atlaskv.server.statemachine;

import java.util.Locale;

/**
 * Utility class for parsing human-readable duration strings.
 */
public final class DurationParser {

    private DurationParser() {
        // Utility constructor
    }

    /**
     * Parses the human-readable duration string into milliseconds.
     *
     * @param durationStr the duration string (e.g. 100ms, 30s, 5m, 2h)
     * @return duration in milliseconds
     */
    public static long parseDurationMs(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return 0;
        }
        String str = durationStr.trim().toLowerCase(Locale.ROOT);
        try {
            if (str.endsWith("ms")) {
                return Long.parseLong(str.substring(0, str.length() - 2));
            } else if (str.endsWith("s")) {
                return Long.parseLong(str.substring(0, str.length() - 1)) * 1000;
            } else if (str.endsWith("m")) {
                return Long.parseLong(str.substring(0, str.length() - 1)) * 60 * 1000;
            } else if (str.endsWith("h")) {
                return Long.parseLong(str.substring(0, str.length() - 1)) * 60 * 60 * 1000;
            } else {
                return Long.parseLong(str) * 1000;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid TTL duration format: " + durationStr);
        }
    }
}
