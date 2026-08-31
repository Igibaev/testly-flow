package com.testlyflow.repository;

import com.testlyflow.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    Optional<Category> findBySlug(String slug);

    @Query("SELECT c FROM Category c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(:name))")
    Optional<Category> findByNameIgnoreCaseTrimmed(@Param("name") String name);

    @Query("""
        SELECT c FROM Category c
        WHERE c.id IN (SELECT DISTINCT q.category.id FROM Question q)
        ORDER BY c.sortOrder ASC, c.name ASC
        """)
    List<Category> findAllWithQuestionsOrdered();
}
