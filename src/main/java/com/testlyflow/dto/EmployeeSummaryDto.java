package com.testlyflow.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EmployeeSummaryDto(
        String firstName,
        String lastName,
        String team,
        long attemptsCount,
        long completedCount,
        BigDecimal avgScorePercent,
        BigDecimal avgTimePerQuestionSeconds,
        OffsetDateTime lastAttemptAt
) {
}
