package com.testlyflow.repository;

import com.testlyflow.entity.Attempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    @Query("""
        SELECT a FROM Attempt a
        WHERE (:team IS NULL OR a.team = :team)
        ORDER BY a.startedAt DESC
        """)
    Page<Attempt> search(@Param("team") String team, Pageable pageable);

    @Query(value = """
        SELECT COUNT(*) FROM attempts a
        WHERE (CAST(:categoryId AS BIGINT) IS NULL OR EXISTS (
                SELECT 1 FROM attempt_answers aa JOIN questions q ON q.id = aa.question_id
                WHERE aa.attempt_id = a.id AND q.category_id = CAST(:categoryId AS BIGINT)))
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        """, nativeQuery = true)
    long countStarts(@Param("categoryId") Long categoryId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT COUNT(*) FROM attempts a
        WHERE a.status = 'COMPLETED'
          AND (CAST(:categoryId AS BIGINT) IS NULL OR EXISTS (
                SELECT 1 FROM attempt_answers aa JOIN questions q ON q.id = aa.question_id
                WHERE aa.attempt_id = a.id AND q.category_id = CAST(:categoryId AS BIGINT)))
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        """, nativeQuery = true)
    long countCompleted(@Param("categoryId") Long categoryId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (finished_at - started_at)))
        FROM attempts a
        WHERE status = 'COMPLETED'
          AND (CAST(:categoryId AS BIGINT) IS NULL OR EXISTS (
                SELECT 1 FROM attempt_answers aa JOIN questions q ON q.id = aa.question_id
                WHERE aa.attempt_id = a.id AND q.category_id = CAST(:categoryId AS BIGINT)))
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        """, nativeQuery = true)
    Double averageDurationSeconds(@Param("categoryId") Long categoryId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT
            CASE
                WHEN score_percent < 21 THEN '0-20'
                WHEN score_percent < 41 THEN '21-40'
                WHEN score_percent < 61 THEN '41-60'
                WHEN score_percent < 81 THEN '61-80'
                ELSE '81-100'
            END AS bucket,
            COUNT(*) AS cnt
        FROM attempts a
        WHERE status = 'COMPLETED'
          AND (CAST(:categoryId AS BIGINT) IS NULL OR EXISTS (
                SELECT 1 FROM attempt_answers aa JOIN questions q ON q.id = aa.question_id
                WHERE aa.attempt_id = a.id AND q.category_id = CAST(:categoryId AS BIGINT)))
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        GROUP BY bucket
        """, nativeQuery = true)
    List<Object[]> scoreDistribution(@Param("categoryId") Long categoryId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
        SELECT team, COUNT(*) AS cnt, AVG(score_percent) AS avg_score
        FROM attempts a
        WHERE status = 'COMPLETED'
          AND (CAST(:categoryId AS BIGINT) IS NULL OR EXISTS (
                SELECT 1 FROM attempt_answers aa JOIN questions q ON q.id = aa.question_id
                WHERE aa.attempt_id = a.id AND q.category_id = CAST(:categoryId AS BIGINT)))
          AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR a.started_at >= CAST(:from AS TIMESTAMPTZ))
          AND (CAST(:to AS TIMESTAMPTZ) IS NULL OR a.started_at <= CAST(:to AS TIMESTAMPTZ))
        GROUP BY team
        ORDER BY team ASC
        """, nativeQuery = true)
    List<Object[]> teamActivity(@Param("categoryId") Long categoryId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    List<Attempt> findByFirstNameAndLastNameAndTeamOrderByStartedAtDesc(String firstName, String lastName, String team);

    /**
     * One row per distinct employee (identified by first name + last name + team, since
     * attempts carry no login). Row shape: first_name, last_name, team, attempts_count,
     * completed_count, avg_score, avg_time_per_question_seconds, last_attempt_at.
     */
    @Query(value = """
        SELECT
            a.first_name AS first_name,
            a.last_name AS last_name,
            a.team AS team,
            COUNT(DISTINCT a.id) AS attempts_count,
            COUNT(DISTINCT CASE WHEN a.status = 'COMPLETED' THEN a.id END) AS completed_count,
            AVG(CASE WHEN a.status = 'COMPLETED' THEN a.score_percent END) AS avg_score,
            (SELECT AVG(aa.time_spent_ms) / 1000.0
                FROM attempt_answers aa
                JOIN attempts a2 ON a2.id = aa.attempt_id
                WHERE a2.first_name = a.first_name AND a2.last_name = a.last_name AND a2.team = a.team
                  AND a2.status = 'COMPLETED' AND a2.timing_suspicious = false AND aa.time_spent_ms > 0
            ) AS avg_time_per_question_seconds,
            MAX(a.started_at) AS last_attempt_at
        FROM attempts a
        GROUP BY a.first_name, a.last_name, a.team
        ORDER BY avg_time_per_question_seconds ASC NULLS LAST
        """, nativeQuery = true)
    List<Object[]> employeeRoster();
}
