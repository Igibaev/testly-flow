package com.testlyflow.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record StartAttemptResponse(
        Long attemptId,
        OffsetDateTime startedAt,
        List<AttemptQuestionDto> questions,
        int totalQuestions,
        int categoriesCount
) {
}
