package com.testlyflow.dto;

import java.util.List;

public record AttemptQuestionDto(
        Long questionId,
        int displayNumber,
        Long categoryId,
        String categoryName,
        String text,
        List<QuestionOptionDto> options
) {
}
