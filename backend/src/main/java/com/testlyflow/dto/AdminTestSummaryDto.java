package com.testlyflow.dto;

import java.time.OffsetDateTime;

public record AdminTestSummaryDto(
        Long id,
        String title,
        String description,
        int questionCount,
        long attemptsCount,
        OffsetDateTime createdAt
) {
}
