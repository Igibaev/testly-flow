package com.testlyflow.dto;

import java.util.List;

public record TestDetailDto(
        Long id,
        String title,
        String description,
        List<PrepLinkDto> prepLinks,
        List<QuestionPublicDto> questions
) {
}
