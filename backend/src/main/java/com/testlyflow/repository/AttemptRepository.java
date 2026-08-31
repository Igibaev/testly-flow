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
}
