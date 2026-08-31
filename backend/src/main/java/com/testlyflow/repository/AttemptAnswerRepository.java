package com.testlyflow.repository;

import com.testlyflow.entity.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttemptIdOrderByDisplayNumberAsc(Long attemptId);

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    /**
     * Per-question timing aggregates across completed, non-suspicious attempts.
     * Row shape: question_id, number, text, category_id, category_name,
     *            avg_seconds, median_seconds, samples_count, correct_rate.
     */
    @Query(value = """
        SELECT
            aa.question_id AS question_id,
            q.number AS number,
            q.text AS text,
            q.category_id AS category_id,
            c.name AS category_name,
            AVG(aa.time_spent_ms) / 1000.0 AS avg_seconds,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY aa.time_spent_ms) / 1000.0 AS median_seconds,
            COUNT(*) AS samples_count,
            (100.0 * SUM(CASE WHEN aa.is_correct THEN 1 ELSE 0 END) / COUNT(*)) AS correct_rate
        FROM attempt_answers aa
        JOIN attempts a ON a.id = aa.attempt_id
        JOIN questions q ON q.id = aa.question_id
        JOIN categories c ON c.id = q.category_id
        WHERE a.status = 'COMPLETED'
          AND a.timing_suspicious = false
          AND aa.time_spent_ms > 0
          AND (CAST(:categoryId AS BIGINT) IS NULL OR q.category_id = CAST(:categoryId AS BIGINT))
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        GROUP BY aa.question_id, q.number, q.text, q.category_id, c.name
        HAVING COUNT(*) >= :minSamples
        """, nativeQuery = true)
    List<Object[]> questionTimingAggregates(@Param("categoryId") Long categoryId,
                                             @Param("from") OffsetDateTime from,
                                             @Param("to") OffsetDateTime to,
                                             @Param("minSamples") int minSamples);

    /**
     * Per-category timing/correctness aggregates across completed, non-suspicious attempts.
     * Row shape: category_id, category_name, color, questions_served, attempts_covered,
     *            avg_seconds, median_seconds, avg_seconds_per_attempt, correct_rate.
     */
    @Query(value = """
        SELECT
            q.category_id AS category_id,
            c.name AS category_name,
            c.color AS color,
            COUNT(*) AS questions_served,
            COUNT(DISTINCT aa.attempt_id) AS attempts_covered,
            AVG(aa.time_spent_ms) / 1000.0 AS avg_seconds,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY aa.time_spent_ms) / 1000.0 AS median_seconds,
            (SELECT AVG(per_attempt.total_ms) / 1000.0
                FROM (
                    SELECT SUM(aa2.time_spent_ms) AS total_ms
                    FROM attempt_answers aa2
                    JOIN attempts a2 ON a2.id = aa2.attempt_id
                    JOIN questions q2 ON q2.id = aa2.question_id
                    WHERE q2.category_id = q.category_id
                      AND a2.status = 'COMPLETED'
                      AND a2.timing_suspicious = false
                      AND aa2.time_spent_ms > 0
                      AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a2.started_at >= CAST(:from AS TIMESTAMPTZ))
                      AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a2.started_at <= CAST(:to AS TIMESTAMPTZ))
                    GROUP BY aa2.attempt_id
                ) per_attempt) AS avg_seconds_per_attempt,
            (100.0 * SUM(CASE WHEN aa.is_correct THEN 1 ELSE 0 END) / COUNT(*)) AS correct_rate
        FROM attempt_answers aa
        JOIN attempts a ON a.id = aa.attempt_id
        JOIN questions q ON q.id = aa.question_id
        JOIN categories c ON c.id = q.category_id
        WHERE a.status = 'COMPLETED'
          AND a.timing_suspicious = false
          AND aa.time_spent_ms > 0
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        GROUP BY q.category_id, c.name, c.color
        """, nativeQuery = true)
    List<Object[]> categoryTimingAggregates(@Param("from") OffsetDateTime from,
                                             @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT COUNT(DISTINCT a.id)
        FROM attempts a
        WHERE a.status = 'COMPLETED' AND a.timing_suspicious = true
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        """, nativeQuery = true)
    long countSuspiciousCompletedAttempts(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /**
     * How long this one employee (first name + last name + team) took on each question they
     * answered, across their completed, non-suspicious attempts. Row shape: question_id, number,
     * text, category_id, category_name, avg_seconds, samples_count, correct_rate.
     */
    @Query(value = """
        SELECT
            aa.question_id AS question_id,
            q.number AS number,
            q.text AS text,
            q.category_id AS category_id,
            c.name AS category_name,
            AVG(aa.time_spent_ms) / 1000.0 AS avg_seconds,
            COUNT(*) AS samples_count,
            (100.0 * SUM(CASE WHEN aa.is_correct THEN 1 ELSE 0 END) / COUNT(*)) AS correct_rate
        FROM attempt_answers aa
        JOIN attempts a ON a.id = aa.attempt_id
        JOIN questions q ON q.id = aa.question_id
        JOIN categories c ON c.id = q.category_id
        WHERE a.status = 'COMPLETED' AND a.timing_suspicious = false AND aa.time_spent_ms > 0
          AND a.first_name = :firstName AND a.last_name = :lastName AND a.team = :team
        GROUP BY aa.question_id, q.number, q.text, q.category_id, c.name
        ORDER BY avg_seconds DESC
        """, nativeQuery = true)
    List<Object[]> employeeQuestionTimings(@Param("firstName") String firstName,
                                            @Param("lastName") String lastName,
                                            @Param("team") String team);

    /**
     * Baseline average time per question across ALL employees (completed, non-suspicious
     * answers), with no min-samples gate -- used only as a comparison reference next to an
     * individual employee's own timing, never as a standalone ranking. Row shape: question_id,
     * avg_seconds, samples_count.
     */
    @Query(value = """
        SELECT aa.question_id AS question_id, AVG(aa.time_spent_ms) / 1000.0 AS avg_seconds, COUNT(*) AS samples_count
        FROM attempt_answers aa
        JOIN attempts a ON a.id = aa.attempt_id
        WHERE a.status = 'COMPLETED' AND a.timing_suspicious = false AND aa.time_spent_ms > 0
        GROUP BY aa.question_id
        """, nativeQuery = true)
    List<Object[]> globalQuestionTimingBaseline();
}
