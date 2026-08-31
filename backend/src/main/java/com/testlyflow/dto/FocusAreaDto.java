package com.testlyflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record FocusAreaDto(
        Long categoryId,
        String categoryName,
        BigDecimal correctRate,
        int wrongCount,
        List<PrepLinkDto> prepLinks
) {
}
