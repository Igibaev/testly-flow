import { useCallback, useEffect, useRef } from 'react';

/**
 * Tracks accumulated time spent per question id. The clock runs only while this
 * question is the active one on screen AND the tab is visible -- switching questions,
 * switching tabs, or closing the tab all pause it. Returns helpers to flush the
 * currently-running question's elapsed time into the accumulator (used right before
 * saving an answer, switching questions, or the tab closing).
 */
export function useQuestionTimer() {
  const accumulatedRef = useRef(new Map()); // questionId -> ms
  const activeQuestionIdRef = useRef(null);
  const resumedAtRef = useRef(null);

  const flush = useCallback(() => {
    const qId = activeQuestionIdRef.current;
    if (qId == null || resumedAtRef.current == null) {
      return;
    }
    const elapsed = Date.now() - resumedAtRef.current;
    const prev = accumulatedRef.current.get(qId) || 0;
    accumulatedRef.current.set(qId, prev + Math.max(elapsed, 0));
    resumedAtRef.current = Date.now();
  }, []);

  const pause = useCallback(() => {
    flush();
    resumedAtRef.current = null;
  }, [flush]);

  const resume = useCallback(() => {
    if (document.visibilityState === 'visible') {
      resumedAtRef.current = Date.now();
    }
  }, []);

  const setActiveQuestion = useCallback(
    (questionId) => {
      flush();
      activeQuestionIdRef.current = questionId;
      resumedAtRef.current = document.visibilityState === 'visible' ? Date.now() : null;
    },
    [flush]
  );

  const getAccumulatedMs = useCallback((questionId) => {
    flush();
    return accumulatedRef.current.get(questionId) || 0;
  }, [flush]);

  useEffect(() => {
    const handleVisibility = () => {
      if (document.visibilityState === 'hidden') {
        pause();
      } else {
        resume();
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [pause, resume]);

  return { setActiveQuestion, getAccumulatedMs, flush, pause };
}
