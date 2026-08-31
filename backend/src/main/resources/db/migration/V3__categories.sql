-- Iteration 2: introduce question categories and rework tests/questions/prep_links around them.
--
-- DESTRUCTIVE MIGRATION: this repository's data model changes shape (categories become
-- the primary grouping for questions instead of a single "test"). Backward compatibility
-- with existing uploaded tests / attempts / metrics is explicitly NOT required for this
-- iteration, so all previously collected data is wiped before the new NOT NULL columns
-- are added. This is intentional -- see README.md for details. Do not run this against
-- a database whose data you need to keep.
TRUNCATE TABLE metrics, attempt_answers, attempts, prep_links, question_options, questions, tests
    RESTART IDENTITY CASCADE;

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    slug VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(16),
    sort_order INTEGER NOT NULL DEFAULT 0,
    questions_min INTEGER,
    questions_max INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- tests becomes an "upload batch" administrative entity, always tied to a category.
ALTER TABLE tests
    ADD COLUMN category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE;

ALTER TABLE tests
    ALTER COLUMN category_id SET NOT NULL;

CREATE INDEX idx_tests_category_id ON tests(category_id);

-- questions now belong to a category directly (in addition to the upload batch they came from).
ALTER TABLE questions
    ADD COLUMN category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE;

ALTER TABLE questions
    ALTER COLUMN category_id SET NOT NULL;

CREATE INDEX idx_questions_category_id ON questions(category_id);

-- prep_links move from being attached to a single uploaded test to being attached to a category.
ALTER TABLE prep_links
    DROP CONSTRAINT prep_links_test_id_fkey;

ALTER TABLE prep_links
    RENAME COLUMN test_id TO category_id;

ALTER TABLE prep_links
    ADD CONSTRAINT prep_links_category_id_fkey FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;

ALTER INDEX idx_prep_links_test_id RENAME TO idx_prep_links_category_id;

-- An attempt is now assembled dynamically from many categories (and therefore many upload
-- batches), so it can no longer reference a single "test". Attempt-level aggregate metrics
-- (starts/completed/abandoned/average duration/score distribution/team activity) are computed
-- on the fly straight from the attempts / attempt_answers tables, so the separate incremental
-- "metrics" counter table (which was keyed one-to-one on a single test) is no longer meaningful
-- and is dropped rather than reshaped.
ALTER TABLE attempts
    DROP CONSTRAINT attempts_test_id_fkey,
    DROP COLUMN test_id;

DROP TABLE metrics;
