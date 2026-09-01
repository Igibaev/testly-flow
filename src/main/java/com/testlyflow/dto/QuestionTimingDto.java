package com.testlyflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuestionTimingDto(
        BigDecimal averageTimePerQuestionSeconds,
        BigDecimal medianTimePerQuestionSeconds,
        List<QuestionTimingItemDto> slowestQuestions,
        List<QuestionTimingItemDto> fastestQuestions
) {
}
