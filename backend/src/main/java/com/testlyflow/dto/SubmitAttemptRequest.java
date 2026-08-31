package com.testlyflow.dto;

import java.util.List;

public record SubmitAttemptRequest(
        List<AnswerSubmission> answers,
        List<TimingSubmission> timings
) {
    public SubmitAttemptRequest {
        answers = answers != null ? answers : List.of();
        timings = timings != null ? timings : List.of();
    }
}
