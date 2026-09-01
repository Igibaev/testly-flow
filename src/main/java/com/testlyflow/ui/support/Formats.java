package com.testlyflow.ui.support;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formats {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss").withLocale(new Locale("ru"));

    private Formats() {
    }

    public static String durationSeconds(Long seconds) {
        if (seconds == null || seconds == 0) {
            return "0 сек";
        }
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes > 0 ? minutes + " мин " + secs + " с" : secs + " с";
    }

    public static String durationMs(Long ms) {
        if (ms == null || ms == 0) {
            return "0 с";
        }
        long totalSeconds = Math.round(ms / 1000.0);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes > 0 ? minutes + " мин " + seconds + " с" : seconds + " с";
    }

    public static String dateTime(OffsetDateTime value) {
        if (value == null) {
            return "—";
        }
        return DATE_TIME.format(value.toLocalDateTime());
    }

    public static String percent(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.stripTrailingZeros().toPlainString() + "%";
    }

    public static String percent(int value) {
        return value + "%";
    }
}
