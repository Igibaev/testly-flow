package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.*;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private static final List<String> SCORE_BUCKETS = List.of("0-20", "21-40", "41-60", "61-80", "81-100");
    private static final int TOP_N = 10;

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TestlyProperties properties;

    public MetricsService(AttemptRepository attemptRepository,
                           AttemptAnswerRepository attemptAnswerRepository,
                           TestlyProperties properties) {
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public MetricsDto getMetrics(Long categoryId, OffsetDateTime from, OffsetDateTime to) {
        long startsCount = attemptRepository.countStarts(categoryId, from, to);
        long completedCount = attemptRepository.countCompleted(categoryId, from, to);
        long abandonedCount = Math.max(startsCount - completedCount, 0);

        Double avgDuration = attemptRepository.averageDurationSeconds(categoryId, from, to);
        long averageDurationSeconds = avgDuration != null ? Math.round(avgDuration) : 0L;

        Map<String, Long> scoreDistribution = new LinkedHashMap<>();
        SCORE_BUCKETS.forEach(bucket -> scoreDistribution.put(bucket, 0L));
        for (Object[] row : attemptRepository.scoreDistribution(categoryId, from, to)) {
            scoreDistribution.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<TeamActivityDto> teamActivity = attemptRepository.teamActivity(categoryId, from, to).stream()
                .map(row -> new TeamActivityDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        row[2] != null ? scale(((Number) row[2]).doubleValue()) : BigDecimal.ZERO))
                .toList();

        int minSamples = properties.getMetrics().getMinSamples();
        List<Object[]> timingRows = attemptAnswerRepository.questionTimingAggregates(categoryId, from, to, minSamples);
        List<QuestionTimingItemDto> allQuestionTimings = timingRows.stream()
                .map(this::toQuestionTimingItem)
                .toList();

        QuestionTimingDto questionTiming = buildQuestionTiming(allQuestionTimings);

        List<Object[]> categoryRows = attemptAnswerRepository.categoryTimingAggregates(from, to);
        List<CategoryMetricsItemDto> categoryMetrics = categoryRows.stream()
                .map(row -> toCategoryMetricsItem(row, allQuestionTimings))
                .toList();

        CategoryRankingDto categoryRanking = buildRanking(categoryMetrics);

        long excludedSuspicious = attemptAnswerRepository.countSuspiciousCompletedAttempts(from, to);

        return new MetricsDto(startsCount, completedCount, abandonedCount, averageDurationSeconds,
                scoreDistribution, teamActivity, questionTiming, categoryMetrics, categoryRanking,
                excludedSuspicious, minSamples);
    }

    private QuestionTimingDto buildQuestionTiming(List<QuestionTimingItemDto> all) {
        if (all.isEmpty()) {
            return new QuestionTimingDto(BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        }

        BigDecimal avgOfAvg = scale(all.stream().mapToDouble(q -> q.avgTimeSeconds().doubleValue()).average().orElse(0));
        BigDecimal medianOfMedian = scale(all.stream().mapToDouble(q -> q.medianTimeSeconds().doubleValue()).average().orElse(0));

        List<QuestionTimingItemDto> slowest = all.stream()
                .sorted(Comparator.comparing(QuestionTimingItemDto::avgTimeSeconds).reversed())
                .limit(TOP_N)
                .toList();
        List<QuestionTimingItemDto> fastest = all.stream()
                .sorted(Comparator.comparing(QuestionTimingItemDto::avgTimeSeconds))
                .limit(TOP_N)
                .toList();

        return new QuestionTimingDto(avgOfAvg, medianOfMedian, slowest, fastest);
    }

    private QuestionTimingItemDto toQuestionTimingItem(Object[] row) {
        return new QuestionTimingItemDto(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).intValue(),
                (String) row[2],
                ((Number) row[3]).longValue(),
                (String) row[4],
                scale(((Number) row[5]).doubleValue()),
                scale(((Number) row[6]).doubleValue()),
                ((Number) row[7]).longValue(),
                scale(((Number) row[8]).doubleValue()));
    }

    private CategoryMetricsItemDto toCategoryMetricsItem(Object[] row, List<QuestionTimingItemDto> allQuestionTimings) {
        Long categoryId = ((Number) row[0]).longValue();
        String categoryName = (String) row[1];
        String color = (String) row[2];
        long questionsServed = ((Number) row[3]).longValue();
        long attemptsCovered = ((Number) row[4]).longValue();
        BigDecimal avgSeconds = scale(((Number) row[5]).doubleValue());
        BigDecimal medianSeconds = scale(((Number) row[6]).doubleValue());
        BigDecimal avgPerAttempt = row[7] != null ? scale(((Number) row[7]).doubleValue()) : BigDecimal.ZERO;
        BigDecimal correctRate = scale(((Number) row[8]).doubleValue());

        List<QuestionTimingItemDto> inCategory = allQuestionTimings.stream()
                .filter(q -> q.categoryId().equals(categoryId))
                .toList();
        QuestionRefDto slowestQuestion = inCategory.stream()
                .max(Comparator.comparing(QuestionTimingItemDto::avgTimeSeconds))
                .map(q -> new QuestionRefDto(q.questionId(), q.number(), q.avgTimeSeconds()))
                .orElse(null);
        QuestionRefDto fastestQuestion = inCategory.stream()
                .min(Comparator.comparing(QuestionTimingItemDto::avgTimeSeconds))
                .map(q -> new QuestionRefDto(q.questionId(), q.number(), q.avgTimeSeconds()))
                .orElse(null);

        return new CategoryMetricsItemDto(categoryId, categoryName, color, questionsServed, attemptsCovered,
                avgSeconds, medianSeconds, avgPerAttempt, correctRate, slowestQuestion, fastestQuestion);
    }

    private CategoryRankingDto buildRanking(List<CategoryMetricsItemDto> categoryMetrics) {
        List<String> slowest = categoryMetrics.stream()
                .sorted(Comparator.comparing(CategoryMetricsItemDto::avgTimePerQuestionSeconds).reversed())
                .map(CategoryMetricsItemDto::categoryName)
                .toList();
        List<String> fastest = categoryMetrics.stream()
                .sorted(Comparator.comparing(CategoryMetricsItemDto::avgTimePerQuestionSeconds))
                .map(CategoryMetricsItemDto::categoryName)
                .toList();
        List<String> weakest = categoryMetrics.stream()
                .sorted(Comparator.comparing(CategoryMetricsItemDto::correctRate))
                .map(CategoryMetricsItemDto::categoryName)
                .toList();
        return new CategoryRankingDto(slowest, fastest, weakest);
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
