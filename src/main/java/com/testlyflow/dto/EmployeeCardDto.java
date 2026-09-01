package com.testlyflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record EmployeeCardDto(
        String firstName,
        String lastName,
        String team,
        BigDecimal avgTimePerQuestionSeconds,
        List<EmployeeAttemptDto> attempts,
        List<EmployeeQuestionTimingDto> questionTimings
) {
}
