package com.testlyflow.repository;

import com.testlyflow.entity.Metrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetricsRepository extends JpaRepository<Metrics, Long> {

    Optional<Metrics> findByTestId(Long testId);
}
