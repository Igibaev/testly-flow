package com.testlyflow.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringCalculatorTest {

    @Test
    void computesExactPercentage() {
        assertEquals(new BigDecimal("50.00"), ScoringCalculator.scorePercent(5, 10));
        assertEquals(new BigDecimal("100.00"), ScoringCalculator.scorePercent(10, 10));
        assertEquals(new BigDecimal("0.00"), ScoringCalculator.scorePercent(0, 10));
    }

    @Test
    void roundsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("33.33"), ScoringCalculator.scorePercent(1, 3));
        assertEquals(new BigDecimal("66.67"), ScoringCalculator.scorePercent(2, 3));
    }

    @Test
    void returnsZeroWhenNoQuestions() {
        assertEquals(BigDecimal.ZERO, ScoringCalculator.scorePercent(0, 0));
    }
}
