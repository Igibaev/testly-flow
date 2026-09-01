package com.testlyflow.ui.support;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Accumulates time spent per question. The clock runs only while a question is active
 * AND the tab is considered visible. Switching questions, hiding the tab, or detaching
 * the UI all pause it. Values are absolute totals, never deltas — flushing twice does
 * not double-count.
 *
 * <p>On UI detach the in-flight interval is cut at the last confirmed client activity
 * (heartbeat or user event), not at {@code now}: otherwise the servlet-session timeout
 * would inflate the last question's time.
 */
public class QuestionTimer {

    private final LongSupplier clock;
    private final Map<Long, Long> accumulated = new HashMap<>();
    private Long activeQuestionId;
    private Long activeSinceMs;
    private Long lastClientActivityMs;
    private boolean paused;

    public QuestionTimer() {
        this(System::currentTimeMillis);
    }

    public QuestionTimer(LongSupplier clock) {
        this.clock = clock;
    }

    public void seed(long questionId, long accumulatedMs) {
        accumulated.put(questionId, Math.max(accumulatedMs, 0L));
    }

    public void setActiveQuestion(Long questionId) {
        flush();
        activeQuestionId = questionId;
        if (!paused && questionId != null) {
            activeSinceMs = now();
            noteActivity();
        } else {
            activeSinceMs = null;
        }
    }

    public long accumulatedMs(Long questionId) {
        if (questionId == null) {
            return 0L;
        }
        flush();
        return accumulated.getOrDefault(questionId, 0L);
    }

    public void pause() {
        flush();
        paused = true;
        activeSinceMs = null;
        noteActivity();
    }

    public void resume() {
        paused = false;
        noteActivity();
        if (activeQuestionId != null) {
            activeSinceMs = now();
        }
    }

    public void noteActivity() {
        lastClientActivityMs = now();
    }

    /**
     * Stop the running interval without counting time after the last client activity.
     * Call this from {@code UI.detach} before persisting.
     */
    public void freezeAtLastActivity() {
        if (activeQuestionId != null && activeSinceMs != null && lastClientActivityMs != null
                && lastClientActivityMs > activeSinceMs) {
            add(activeQuestionId, lastClientActivityMs - activeSinceMs);
        }
        activeSinceMs = null;
    }

    public boolean isPaused() {
        return paused;
    }

    public Long activeQuestionId() {
        return activeQuestionId;
    }

    private void flush() {
        if (activeQuestionId == null || activeSinceMs == null || paused) {
            return;
        }
        long elapsed = now() - activeSinceMs;
        add(activeQuestionId, Math.max(elapsed, 0L));
        activeSinceMs = now();
    }

    private void add(Long questionId, long extraMs) {
        accumulated.merge(questionId, extraMs, Long::sum);
    }

    private long now() {
        return clock.getAsLong();
    }
}
