package com.testlyflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAttemptRequest(
        @NotEmpty(message = "нет ни одного ответа") @Valid List<AnswerSubmission> answers
) {
}
