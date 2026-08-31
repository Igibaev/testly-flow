package com.testlyflow.dto;

import java.math.BigDecimal;

public record TeamActivityDto(String team, long attempts, BigDecimal avgScore) {
}
