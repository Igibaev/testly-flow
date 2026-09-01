package com.testlyflow.dto;

import java.time.OffsetDateTime;

public record AdminTestSummaryDto(
        Long id,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        int questionCount,
        OffsetDateTime createdAt
) {
}
