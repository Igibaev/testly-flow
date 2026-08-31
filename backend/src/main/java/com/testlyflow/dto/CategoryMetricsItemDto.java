package com.testlyflow.dto;

import java.math.BigDecimal;

public record CategoryMetricsItemDto(
        Long categoryId,
        String categoryName,
        String color,
        long questionsServed,
        long attemptsCovered,
        BigDecimal avgTimePerQuestionSeconds,
        BigDecimal medianTimePerQuestionSeconds,
        BigDecimal avgTimePerAttemptSeconds,
        BigDecimal correctRate,
        QuestionRefDto slowestQuestion,
        QuestionRefDto fastestQuestion
) {
}
