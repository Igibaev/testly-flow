package com.testlyflow.dto;

public record AnswerDetailDto(
        Long questionId,
        int number,
        String questionText,
        Long categoryId,
        String categoryName,
        String selectedOption,
        String correctOption,
        boolean isCorrect,
        long timeSpentMs
) {
}
