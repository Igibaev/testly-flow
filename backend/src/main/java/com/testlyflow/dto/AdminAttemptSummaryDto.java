package com.testlyflow.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminAttemptSummaryDto(
        Long id,
        Long testId,
        String testTitle,
        String firstName,
        String lastName,
        String team,
        String ipAddress,
        String userAgent,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String status,
        Integer correctCount,
        Integer totalQuestions,
        BigDecimal scorePercent
) {
}
