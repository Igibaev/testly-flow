package com.testlyflow.dto;

import java.math.BigDecimal;

public record QuestionRefDto(Long questionId, int number, BigDecimal avgTimeSeconds) {
}
