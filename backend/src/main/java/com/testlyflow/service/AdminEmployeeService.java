package com.testlyflow.service;

import com.testlyflow.dto.*;
import com.testlyflow.entity.Attempt;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminEmployeeService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    public AdminEmployeeService(AttemptRepository attemptRepository, AttemptAnswerRepository attemptAnswerRepository) {
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeSummaryDto> roster() {
        return attemptRepository.employeeRoster().stream()
                .map(row -> new EmployeeSummaryDto(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue(),
                        row[5] != null ? scale(((Number) row[5]).doubleValue()) : null,
                        row[6] != null ? scale(((Number) row[6]).doubleValue()) : null,
                        toOffsetDateTime(row[7])))
                .toList();
    }

    /**
     * The native aggregate for the roster's "last attempt" column comes back as either
     * {@link java.time.OffsetDateTime} or {@link java.time.Instant} depending on the JDBC
     * driver's handling of MAX() over a TIMESTAMPTZ column -- normalize both to UTC.
     */
    private java.time.OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof java.time.Instant instant) {
            return instant.atOffset(java.time.ZoneOffset.UTC);
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
        throw new IllegalStateException("Unexpected timestamp type: " + value.getClass());
    }

    @Transactional(readOnly = true)
    public EmployeeCardDto getCard(String firstName, String lastName, String team) {
        List<Attempt> attempts = attemptRepository.findByFirstNameAndLastNameAndTeamOrderByStartedAtDesc(firstName, lastName, team);
        if (attempts.isEmpty()) {
            throw new NotFoundException("Сотрудник " + firstName + " " + lastName + " (" + team + ") не найден");
        }

        List<EmployeeAttemptDto> attemptDtos = attempts.stream()
                .map(a -> new EmployeeAttemptDto(
                        a.getId(), a.getStartedAt(), a.getFinishedAt(), a.getStatus().name(),
                        a.getCorrectCount(), a.getTotalQuestions(), a.getScorePercent(), a.isTimingSuspicious()))
                .toList();

        Map<Long, double[]> globalBaseline = new HashMap<>(); // questionId -> [avgSeconds, samples]
        for (Object[] row : attemptAnswerRepository.globalQuestionTimingBaseline()) {
            Long questionId = ((Number) row[0]).longValue();
            globalBaseline.put(questionId, new double[]{((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue()});
        }

        List<EmployeeQuestionTimingDto> questionTimings = attemptAnswerRepository
                .employeeQuestionTimings(firstName, lastName, team).stream()
                .map(row -> {
                    Long questionId = ((Number) row[0]).longValue();
                    double[] baseline = globalBaseline.get(questionId);
                    return new EmployeeQuestionTimingDto(
                            questionId,
                            ((Number) row[1]).intValue(),
                            (String) row[2],
                            ((Number) row[3]).longValue(),
                            (String) row[4],
                            scale(((Number) row[5]).doubleValue()),
                            ((Number) row[6]).longValue(),
                            scale(((Number) row[7]).doubleValue()),
                            baseline != null ? scale(baseline[0]) : null,
                            baseline != null ? (long) baseline[1] : 0L);
                })
                .sorted(Comparator.comparing(EmployeeQuestionTimingDto::employeeAvgSeconds).reversed())
                .toList();

        java.util.OptionalDouble avgOpt = questionTimings.stream()
                .mapToDouble(q -> q.employeeAvgSeconds().doubleValue())
                .average();
        Double avgTimePerQuestion = avgOpt.isPresent() ? avgOpt.getAsDouble() : null;

        return new EmployeeCardDto(firstName, lastName, team,
                avgTimePerQuestion != null ? scale(avgTimePerQuestion) : null,
                attemptDtos, questionTimings);
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
