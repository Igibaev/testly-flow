package com.testlyflow.ui.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatsTest {

    @Test
    void durationSecondsBoundaries() {
        assertEquals("0 сек", Formats.durationSeconds(0L));
        assertEquals("0 сек", Formats.durationSeconds(null));
        assertEquals("59 с", Formats.durationSeconds(59L));
        assertEquals("1 мин 0 с", Formats.durationSeconds(60L));
        assertEquals("1 мин 1 с", Formats.durationSeconds(61L));
        assertEquals("60 мин 0 с", Formats.durationSeconds(3600L));
    }

    @Test
    void durationMsRounding() {
        assertEquals("0 с", Formats.durationMs(0L));
        assertEquals("1 с", Formats.durationMs(1000L));
        assertEquals("1 мин 0 с", Formats.durationMs(60_000L));
    }
}
