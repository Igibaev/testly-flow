package com.testlyflow.service;

import com.testlyflow.dto.MetricsDto;
import com.testlyflow.dto.TeamActivityDto;
import com.testlyflow.entity.Metrics;
import com.testlyflow.repository.AttemptRepository;
import com.testlyflow.repository.MetricsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private static final List<String> SCORE_BUCKETS = List.of("0-20", "21-40", "41-60", "61-80", "81-100");

    private final MetricsRepository metricsRepository;
    private final AttemptRepository attemptRepository;

    public MetricsService(MetricsRepository metricsRepository, AttemptRepository attemptRepository) {
        this.metricsRepository = metricsRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional
    public void recordStart(Long testId) {
        Metrics metrics = getOrCreate(testId);
        metrics.setStartsCount(metrics.getStartsCount() + 1);
        metricsRepository.save(metrics);
    }

    @Transactional
    public void recordCompletion(Long testId, long durationSeconds) {
        Metrics metrics = getOrCreate(testId);
        metrics.setCompletedCount(metrics.getCompletedCount() + 1);
        metrics.setTotalDurationSeconds(metrics.getTotalDurationSeconds() + Math.max(durationSeconds, 0));
        metricsRepository.save(metrics);
    }

    private Metrics getOrCreate(Long testId) {
        return metricsRepository.findByTestId(testId).orElseGet(() -> {
            Metrics metrics = new Metrics();
            metrics.setTestId(testId);
            return metrics;
        });
    }

    @Transactional(readOnly = true)
    public MetricsDto getMetrics(Long testId) {
        long startsCount;
        long completedCount;

        if (testId != null) {
            Metrics metrics = metricsRepository.findByTestId(testId).orElse(null);
            startsCount = metrics != null ? metrics.getStartsCount() : 0;
            completedCount = metrics != null ? metrics.getCompletedCount() : 0;
        } else {
            List<Metrics> all = metricsRepository.findAll();
            startsCount = all.stream().mapToLong(Metrics::getStartsCount).sum();
            completedCount = all.stream().mapToLong(Metrics::getCompletedCount).sum();
        }

        long abandonedCount = Math.max(startsCount - completedCount, 0);

        Double avgDuration = attemptRepository.averageDurationSeconds(testId);
        long averageDurationSeconds = avgDuration != null ? Math.round(avgDuration) : 0L;

        Map<String, Long> scoreDistribution = new LinkedHashMap<>();
        SCORE_BUCKETS.forEach(bucket -> scoreDistribution.put(bucket, 0L));
        for (Object[] row : attemptRepository.scoreDistribution(testId)) {
            scoreDistribution.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<TeamActivityDto> teamActivity = attemptRepository.teamActivity(testId).stream()
                .map(row -> new TeamActivityDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] != null ? BigDecimal.valueOf(((Number) row[2]).doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO))
                .toList();

        return new MetricsDto(testId, startsCount, completedCount, abandonedCount, averageDurationSeconds,
                scoreDistribution, teamActivity);
    }
}
