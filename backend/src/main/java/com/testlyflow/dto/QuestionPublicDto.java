package com.testlyflow.dto;

import java.util.List;

public record QuestionPublicDto(Long id, int number, String text, List<QuestionOptionDto> options) {
}
