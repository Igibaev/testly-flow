-- Iteration 2: per-question timing metrics and the fixed composition of an in-progress attempt.
ALTER TABLE attempt_answers
    ADD COLUMN display_number INTEGER,
    ADD COLUMN time_spent_ms BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN answered_at TIMESTAMPTZ,
    ADD COLUMN visits_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE attempts
    ADD COLUMN timing_suspicious BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_attempt_answers_question_id ON attempt_answers(question_id);
CREATE INDEX idx_attempt_answers_attempt_display_number ON attempt_answers(attempt_id, display_number);
