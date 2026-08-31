package com.testlyflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record SubmitAttemptResponse(
        Long attemptId,
        int correctCount,
        int totalQuestions,
        BigDecimal scorePercent,
        String resultTier,
        String headline,
        String message,
        List<FocusAreaDto> focusAreas,
        List<AnswerDetailDto> details
) {
}
