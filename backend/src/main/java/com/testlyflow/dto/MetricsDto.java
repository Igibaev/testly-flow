package com.testlyflow.dto;

import java.util.List;
import java.util.Map;

public record MetricsDto(
        Long testId,
        long startsCount,
        long completedCount,
        long abandonedCount,
        long averageDurationSeconds,
        Map<String, Long> scoreDistribution,
        List<TeamActivityDto> teamActivity
) {
}
