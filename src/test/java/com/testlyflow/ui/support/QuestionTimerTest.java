package com.testlyflow.ui.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionTimerTest {

    @Test
    void accumulatesPerQuestionWithoutDoubleCount() {
        AtomicLong now = new AtomicLong(1_000);
        QuestionTimer timer = new QuestionTimer(now::get);
        timer.setActiveQuestion(1L);
        now.addAndGet(400);
        assertEquals(400, timer.accumulatedMs(1L));
        assertEquals(400, timer.accumulatedMs(1L));
        timer.setActiveQuestion(2L);
        now.addAndGet(250);
        assertEquals(400, timer.accumulatedMs(1L));
        assertEquals(250, timer.accumulatedMs(2L));
    }

    @Test
    void pauseStopsTheClock() {
        AtomicLong now = new AtomicLong(0);
        QuestionTimer timer = new QuestionTimer(now::get);
        timer.setActiveQuestion(5L);
        now.addAndGet(100);
        timer.pause();
        now.addAndGet(5_000);
        assertEquals(100, timer.accumulatedMs(5L));
        assertTrue(timer.isPaused());
        timer.resume();
        now.addAndGet(50);
        assertEquals(150, timer.accumulatedMs(5L));
    }

    @Test
    void seedRestoresSavedTotals() {
        AtomicLong now = new AtomicLong(0);
        QuestionTimer timer = new QuestionTimer(now::get);
        timer.seed(9L, 1_200);
        timer.setActiveQuestion(9L);
        now.addAndGet(80);
        assertEquals(1_280, timer.accumulatedMs(9L));
    }

    @Test
    void freezeAtLastActivityIgnoresIdleAfterLastHeartbeat() {
        AtomicLong now = new AtomicLong(0);
        QuestionTimer timer = new QuestionTimer(now::get);
        timer.setActiveQuestion(3L);
        now.addAndGet(200);
        timer.noteActivity();
        now.addAndGet(10_000);
        timer.freezeAtLastActivity();
        assertEquals(200, timer.accumulatedMs(3L));
    }
}
