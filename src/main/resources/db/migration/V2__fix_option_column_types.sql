ALTER TABLE questions
    ALTER COLUMN correct_option TYPE VARCHAR(1) USING correct_option::VARCHAR(1);

ALTER TABLE question_options
    ALTER COLUMN option_letter TYPE VARCHAR(1) USING option_letter::VARCHAR(1);

ALTER TABLE attempt_answers
    ALTER COLUMN selected_option TYPE VARCHAR(1) USING selected_option::VARCHAR(1);
