package com.testlyflow.dto;

import java.util.List;

public record CategoryRankingDto(
        List<String> slowestCategories,
        List<String> fastestCategories,
        List<String> weakestCategories
) {
}
