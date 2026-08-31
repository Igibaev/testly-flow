package com.testlyflow.dto;

import java.math.BigDecimal;

public record QuestionTimingItemDto(
        Long questionId,
        int number,
        String text,
        Long categoryId,
        String categoryName,
        BigDecimal avgTimeSeconds,
        BigDecimal medianTimeSeconds,
        long samplesCount,
        BigDecimal correctRate
) {
}
