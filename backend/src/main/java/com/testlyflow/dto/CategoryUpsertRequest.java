package com.testlyflow.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpsertRequest(
        @NotBlank(message = "укажите название категории") String name,
        String description,
        String color,
        Integer sortOrder,
        Integer questionsMin,
        Integer questionsMax
) {
}
