package com.testlyflow.repository;

import com.testlyflow.entity.Attempt;
import com.testlyflow.entity.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    @Query("""
        SELECT a FROM Attempt a
        WHERE (:testId IS NULL OR a.test.id = :testId)
          AND (:team IS NULL OR a.team = :team)
        ORDER BY a.startedAt DESC
        """)
    Page<Attempt> search(@Param("testId") Long testId, @Param("team") String team, Pageable pageable);

    long countByTestIdAndStatus(Long testId, AttemptStatus status);

    long countByStatus(AttemptStatus status);

    long countByTestId(Long testId);

    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (finished_at - started_at)))
        FROM attempts
        WHERE status = 'COMPLETED' AND (:testId IS NULL OR test_id = :testId)
        """, nativeQuery = true)
    Double averageDurationSeconds(@Param("testId") Long testId);

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
        FROM attempts
        WHERE status = 'COMPLETED' AND (:testId IS NULL OR test_id = :testId)
        GROUP BY bucket
        """, nativeQuery = true)
    List<Object[]> scoreDistribution(@Param("testId") Long testId);

    @Query(value = """
        SELECT team, COUNT(*) AS cnt, AVG(score_percent) AS avg_score
        FROM attempts
        WHERE status = 'COMPLETED' AND (:testId IS NULL OR test_id = :testId)
        GROUP BY team
        ORDER BY team ASC
        """, nativeQuery = true)
    List<Object[]> teamActivity(@Param("testId") Long testId);
}
