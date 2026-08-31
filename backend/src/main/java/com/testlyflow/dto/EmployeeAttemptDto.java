package com.testlyflow.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EmployeeAttemptDto(
        Long id,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String status,
        Integer correctCount,
        Integer totalQuestions,
        BigDecimal scorePercent,
        boolean timingSuspicious
) {
}
