package com.testlyflow.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerSubmission(
        @NotNull(message = "не указан questionId") Long questionId,
        String selectedOption
) {
}
