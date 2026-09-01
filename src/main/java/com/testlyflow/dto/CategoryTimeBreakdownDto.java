package com.testlyflow.dto;

public record CategoryTimeBreakdownDto(Long categoryId, String categoryName, long totalTimeSpentMs, int questionCount) {
}
