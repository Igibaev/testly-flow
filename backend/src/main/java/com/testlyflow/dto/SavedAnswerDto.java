package com.testlyflow.dto;

public record SavedAnswerDto(Long questionId, String selectedOption, long timeSpentMs) {
}
