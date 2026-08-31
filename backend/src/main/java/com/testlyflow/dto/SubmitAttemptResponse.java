package com.testlyflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record SubmitAttemptResponse(
        Long attemptId,
        int correctCount,
        int totalQuestions,
        BigDecimal scorePercent,
        List<AnswerDetailDto> details
) {
}
