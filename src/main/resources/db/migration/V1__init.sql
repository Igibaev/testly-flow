CREATE TABLE tests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    number INTEGER NOT NULL,
    text TEXT NOT NULL,
    correct_option CHAR(1) NOT NULL,
    CONSTRAINT uq_questions_test_number UNIQUE (test_id, number)
);

CREATE INDEX idx_questions_test_id ON questions(test_id);

CREATE TABLE question_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_letter CHAR(1) NOT NULL,
    text TEXT NOT NULL,
    CONSTRAINT uq_question_options_question_letter UNIQUE (question_id, option_letter)
);

CREATE INDEX idx_question_options_question_id ON question_options(question_id);

CREATE TABLE prep_links (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(2000) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_prep_links_test_id ON prep_links(test_id);

CREATE TABLE attempts (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    first_name VARCHAR(200) NOT NULL,
    last_name VARCHAR(200) NOT NULL,
    team VARCHAR(200) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    correct_count INTEGER,
    total_questions INTEGER,
    score_percent NUMERIC(5,2)
);

CREATE INDEX idx_attempts_test_id ON attempts(test_id);
CREATE INDEX idx_attempts_team ON attempts(team);

CREATE TABLE attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    selected_option CHAR(1),
    is_correct BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_attempt_answers_attempt_id ON attempt_answers(attempt_id);

CREATE TABLE metrics (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL UNIQUE REFERENCES tests(id) ON DELETE CASCADE,
    starts_count INTEGER NOT NULL DEFAULT 0,
    completed_count INTEGER NOT NULL DEFAULT 0,
    abandoned_count INTEGER NOT NULL DEFAULT 0,
    total_duration_seconds BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
