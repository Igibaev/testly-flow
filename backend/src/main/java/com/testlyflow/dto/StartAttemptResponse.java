package com.testlyflow.dto;

import java.time.OffsetDateTime;

public record StartAttemptResponse(Long attemptId, OffsetDateTime startedAt) {
}
