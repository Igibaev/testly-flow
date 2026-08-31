package com.testlyflow.service;

import com.testlyflow.dto.EmployeeCardDto;
import com.testlyflow.entity.Attempt;
import com.testlyflow.entity.AttemptStatus;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminEmployeeServiceTest {

    private AttemptRepository attemptRepository;
    private AttemptAnswerRepository attemptAnswerRepository;
    private AdminEmployeeService service;

    @BeforeEach
    void setUp() {
        attemptRepository = mock(AttemptRepository.class);
        attemptAnswerRepository = mock(AttemptAnswerRepository.class);
        service = new AdminEmployeeService(attemptRepository, attemptAnswerRepository);
    }

    @Test
    void cardPairsEmployeeTimingWithTheGlobalBaselineForTheSameQuestion() {
        Attempt attempt = new Attempt();
        attempt.setId(1L);
        attempt.setFirstName("Ivan");
        attempt.setLastName("Petrov");
        attempt.setTeam("QA");
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setStartedAt(OffsetDateTime.now().minusMinutes(10));
        when(attemptRepository.findByFirstNameAndLastNameAndTeamOrderByStartedAtDesc("Ivan", "Petrov", "QA"))
                .thenReturn(List.of(attempt));

        // employee took 10s on question 42, samples=1, correctRate=100
        Object[] employeeRow = {42L, 7, "Q text", 3L, "Cat", 10.0, 1L, 100.0};
        when(attemptAnswerRepository.employeeQuestionTimings("Ivan", "Petrov", "QA"))
                .thenReturn(java.util.Collections.singletonList(employeeRow));

        // global baseline: avg 40s across 20 samples for the same question
        Object[] baselineRow = {42L, 40.0, 20L};
        when(attemptAnswerRepository.globalQuestionTimingBaseline()).thenReturn(java.util.Collections.singletonList(baselineRow));

        EmployeeCardDto card = service.getCard("Ivan", "Petrov", "QA");

        assertEquals(1, card.questionTimings().size());
        var timing = card.questionTimings().get(0);
        assertEquals(0, timing.employeeAvgSeconds().compareTo(new java.math.BigDecimal("10.00")));
        assertEquals(0, timing.globalAvgSeconds().compareTo(new java.math.BigDecimal("40.00")));
        assertEquals(20L, timing.globalSamples());
    }

    @Test
    void missingGlobalBaselineForAQuestionLeavesItNullRatherThanFabricatingAValue() {
        Attempt attempt = new Attempt();
        attempt.setId(1L);
        attempt.setFirstName("Ivan");
        attempt.setLastName("Petrov");
        attempt.setTeam("QA");
        attempt.setStatus(AttemptStatus.COMPLETED);
        attempt.setStartedAt(OffsetDateTime.now());
        when(attemptRepository.findByFirstNameAndLastNameAndTeamOrderByStartedAtDesc("Ivan", "Petrov", "QA"))
                .thenReturn(List.of(attempt));

        Object[] employeeRow = {42L, 7, "Q text", 3L, "Cat", 10.0, 1L, 100.0};
        when(attemptAnswerRepository.employeeQuestionTimings("Ivan", "Petrov", "QA"))
                .thenReturn(java.util.Collections.singletonList(employeeRow));
        when(attemptAnswerRepository.globalQuestionTimingBaseline()).thenReturn(List.of());

        EmployeeCardDto card = service.getCard("Ivan", "Petrov", "QA");

        assertNull(card.questionTimings().get(0).globalAvgSeconds());
        assertEquals(0L, card.questionTimings().get(0).globalSamples());
    }
}
