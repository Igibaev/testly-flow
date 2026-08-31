package com.testlyflow.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "metrics")
public class Metrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false, unique = true)
    private Long testId;

    @Column(name = "starts_count", nullable = false)
    private Integer startsCount = 0;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;

    @Column(name = "abandoned_count", nullable = false)
    private Integer abandonedCount = 0;

    @Column(name = "total_duration_seconds", nullable = false)
    private Long totalDurationSeconds = 0L;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Integer getStartsCount() {
        return startsCount;
    }

    public void setStartsCount(Integer startsCount) {
        this.startsCount = startsCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getAbandonedCount() {
        return abandonedCount;
    }

    public void setAbandonedCount(Integer abandonedCount) {
        this.abandonedCount = abandonedCount;
    }

    public Long getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(Long totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
