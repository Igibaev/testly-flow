package com.testlyflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ScoringCalculator {

    private ScoringCalculator() {
    }

    public static BigDecimal scorePercent(int correctCount, int totalQuestions) {
        if (totalQuestions <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP);
    }
}
