package com.testlyflow.dto;

public record AdminCategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        String color,
        long questionCount,
        long testCount,
        int sortOrder,
        Integer questionsMin,
        Integer questionsMax
) {
}
