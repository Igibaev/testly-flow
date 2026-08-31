package com.testlyflow.repository;

import com.testlyflow.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTestIdOrderByNumberAsc(Long testId);

    long countByCategoryId(Long categoryId);

    @Query(value = "SELECT id FROM questions WHERE category_id = :categoryId ORDER BY random() LIMIT :limit",
            nativeQuery = true)
    List<Long> sampleRandomIdsByCategory(@Param("categoryId") Long categoryId, @Param("limit") int limit);
}
