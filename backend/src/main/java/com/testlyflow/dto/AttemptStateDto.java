package com.testlyflow.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AttemptStateDto(
        Long attemptId,
        OffsetDateTime startedAt,
        List<AttemptQuestionDto> questions,
        int totalQuestions,
        int categoriesCount,
        List<SavedAnswerDto> answers
) {
}
