package com.testlyflow.service;

import com.testlyflow.config.TestlyProperties;
import com.testlyflow.dto.MetricsDto;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MetricsServiceTest {

    private AttemptRepository attemptRepository;
    private AttemptAnswerRepository attemptAnswerRepository;
    private TestlyProperties properties;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        attemptRepository = mock(AttemptRepository.class);
        attemptAnswerRepository = mock(AttemptAnswerRepository.class);
        properties = new TestlyProperties();
        properties.getMetrics().setMinSamples(5);
        metricsService = new MetricsService(attemptRepository, attemptAnswerRepository, properties);

        when(attemptRepository.countStarts(any(), any(), any())).thenReturn(0L);
        when(attemptRepository.countCompleted(any(), any(), any())).thenReturn(0L);
        when(attemptRepository.averageDurationSeconds(any(), any(), any())).thenReturn(null);
        when(attemptRepository.scoreDistribution(any(), any(), any())).thenReturn(List.of());
        when(attemptRepository.teamActivity(any(), any(), any())).thenReturn(List.of());
        when(attemptAnswerRepository.categoryTimingAggregates(any(), any())).thenReturn(List.of());
        when(attemptAnswerRepository.countSuspiciousCompletedAttempts(any(), any())).thenReturn(0L);
    }

    @Test
    void passesConfiguredMinSamplesThresholdToTheAggregateQuery() {
        when(attemptAnswerRepository.questionTimingAggregates(any(), any(), any(), eq(5))).thenReturn(List.of());

        metricsService.getMetrics(null, null, null);

        verify(attemptAnswerRepository).questionTimingAggregates(any(), any(), any(), eq(5));
    }

    @Test
    void returnsEmptyTimingListsAndHonestZerosWhenNoQuestionHasEnoughSamples() {
        when(attemptAnswerRepository.questionTimingAggregates(any(), any(), any(), anyInt())).thenReturn(List.of());

        MetricsDto dto = metricsService.getMetrics(null, null, null);

        assertTrue(dto.questionTiming().slowestQuestions().isEmpty());
        assertTrue(dto.questionTiming().fastestQuestions().isEmpty());
    }

    @Test
    void ordersSlowestAndFastestQuestionsByAverageTime() {
        Object[] rowFast = {1L, 1, "Q1", 10L, "Cat", 5.0, 5.0, 6L, 80.0};
        Object[] rowSlow = {2L, 2, "Q2", 10L, "Cat", 90.0, 85.0, 7L, 40.0};
        when(attemptAnswerRepository.questionTimingAggregates(any(), any(), any(), anyInt()))
                .thenReturn(List.of(rowFast, rowSlow));

        MetricsDto dto = metricsService.getMetrics(null, null, null);

        assertEquals(2L, dto.questionTiming().slowestQuestions().get(0).questionId());
        assertEquals(1L, dto.questionTiming().fastestQuestions().get(0).questionId());
    }

    @Test
    void excludesSuspiciousAttemptsCountIsSurfacedSeparately() {
        when(attemptAnswerRepository.countSuspiciousCompletedAttempts(any(), any())).thenReturn(3L);

        MetricsDto dto = metricsService.getMetrics(null, null, null);

        assertEquals(3L, dto.excludedSuspiciousAttempts());
    }
}
