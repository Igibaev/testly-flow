package com.testlyflow.repository;

import com.testlyflow.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);
}
