package com.testlyflow.dto;

import java.math.BigDecimal;

public record EmployeeQuestionTimingDto(
        Long questionId,
        int number,
        String text,
        Long categoryId,
        String categoryName,
        BigDecimal employeeAvgSeconds,
        long employeeSamples,
        BigDecimal employeeCorrectRate,
        BigDecimal globalAvgSeconds,
        long globalSamples
) {
}
