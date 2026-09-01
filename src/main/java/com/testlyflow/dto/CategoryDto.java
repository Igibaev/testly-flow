package com.testlyflow.dto;

import java.util.List;

public record CategoryDto(
        Long id,
        String name,
        String description,
        String color,
        long questionCount,
        List<PrepLinkDto> prepLinks
) {
}
