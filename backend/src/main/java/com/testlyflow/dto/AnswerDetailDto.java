package com.testlyflow.dto;

public record AnswerDetailDto(
        Long questionId,
        int number,
        String questionText,
        String selectedOption,
        String correctOption,
        boolean isCorrect
) {
}
