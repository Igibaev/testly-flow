package com.testlyflow.dto;

import java.util.List;

public record AdminAttemptDetailDto(
        AdminAttemptSummaryDto summary,
        List<AnswerDetailDto> answers,
        AnswerDetailDto slowestQuestion,
        List<CategoryTimeBreakdownDto> categoryBreakdown
) {
}
