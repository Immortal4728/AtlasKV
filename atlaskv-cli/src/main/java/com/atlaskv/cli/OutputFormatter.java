package com.atlaskv.cli;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class for producing formatted, colored console output.
 */
public final class OutputFormatter {

    // ANSI color codes
    private static final String RESET = "\033[0m";
    private static final String GREEN = "\033[32m";
    private static final String RED = "\033[31m";
    private static final String YELLOW = "\033[33m";
    private static final String CYAN = "\033[36m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private OutputFormatter() {
    }

    /**
     * Prints a success message in green.
     *
     * @param message success message
     */
    public static void printSuccess(String message) {
        System.out.println(GREEN + "✓ " + message + RESET);
    }

    /**
     * Prints an error message in red.
     *
     * @param message error message
     */
    public static void printError(String message) {
        System.err.println(RED + "✗ Error: " + message + RESET);
    }

    /**
     * Prints a warning message in yellow.
     *
     * @param message warning message
     */
    public static void printWarning(String message) {
        System.err.println(YELLOW + "⚠ " + message + RESET);
    }

    /**
     * Prints an informational message in cyan.
     *
     * @param message info message
     */
    public static void printInfo(String message) {
        System.out.println(CYAN + "ℹ " + message + RESET);
    }

    /**
     * Prints a key-value pair formatted as a labeled row.
     *
     * @param label the label
     * @param value the value
     */
    public static void printField(String label, Object value) {
        System.out.printf("  %s%-14s%s %s%n", DIM, label + ":", RESET, value != null ? value : "—");
    }

    /**
     * Prints a section header.
     *
     * @param title section title
     */
    public static void printHeader(String title) {
        System.out.println();
        System.out.println(BOLD + CYAN + "─── " + title + " ───" + RESET);
    }

    /**
     * Prints a horizontal separator line.
     */
    public static void printSeparator() {
        System.out.println(DIM + "  ─────────────────────────────────────────" + RESET);
    }

    /**
     * Formats an epoch millisecond timestamp to a human-readable string.
     *
     * @param epochMs epoch milliseconds
     * @return formatted date string, or "—" if null
     */
    public static String formatTimestamp(Long epochMs) {
        if (epochMs == null || epochMs == 0) {
            return "—";
        }
        return TIME_FMT.format(Instant.ofEpochMilli(epochMs));
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     *
     * @param ms duration in milliseconds
     * @return formatted duration
     */
    public static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        }
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remSecs = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + remSecs + "s";
        }
        long hours = minutes / 60;
        long remMins = minutes % 60;
        return hours + "h " + remMins + "m";
    }

    /**
     * Prints a formatted table with column headers and rows.
     *
     * @param headers column headers
     * @param rows    list of row arrays
     */
    public static void printTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < cols && i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i] != null ? row[i].length() : 1);
            }
        }

        // Header
        StringBuilder headerLine = new StringBuilder("  ");
        StringBuilder dashLine = new StringBuilder("  ");
        for (int i = 0; i < cols; i++) {
            headerLine.append(String.format(BOLD + "%-" + (widths[i] + 2) + "s" + RESET, headers[i]));
            dashLine.append("─".repeat(widths[i])).append("  ");
        }
        System.out.println(headerLine);
        System.out.println(DIM + dashLine + RESET);

        // Rows
        for (String[] row : rows) {
            StringBuilder rowLine = new StringBuilder("  ");
            for (int i = 0; i < cols; i++) {
                String val = (i < row.length && row[i] != null) ? row[i] : "—";
                rowLine.append(String.format("%-" + (widths[i] + 2) + "s", val));
            }
            System.out.println(rowLine);
        }
    }

    /**
     * Prints a JSON string with basic pretty formatting.
     *
     * @param json JSON string
     */
    public static void printJson(String json) {
        System.out.println(CYAN + json + RESET);
    }
}
