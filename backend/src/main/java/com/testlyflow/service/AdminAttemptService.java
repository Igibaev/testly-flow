package com.testlyflow.service;

import com.testlyflow.dto.*;
import com.testlyflow.entity.Attempt;
import com.testlyflow.entity.AttemptAnswer;
import com.testlyflow.exception.NotFoundException;
import com.testlyflow.repository.AttemptAnswerRepository;
import com.testlyflow.repository.AttemptRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAttemptService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    public AdminAttemptService(AttemptRepository attemptRepository, AttemptAnswerRepository attemptAnswerRepository) {
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAttemptSummaryDto> search(String team, int page, int size) {
        Page<Attempt> result = attemptRepository.search(team, PageRequest.of(page, size));
        List<AdminAttemptSummaryDto> content = result.getContent().stream()
                .map(this::toSummary)
                .toList();
        return new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminAttemptDetailDto getDetail(Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Попытка с id=" + attemptId + " не найдена"));

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptIdOrderByDisplayNumberAsc(attemptId);
        List<AnswerDetailDto> details = answers.stream()
                .map(a -> new AnswerDetailDto(
                        a.getQuestion().getId(),
                        a.getQuestion().getNumber(),
                        a.getQuestion().getText(),
                        a.getQuestion().getCategory().getId(),
                        a.getQuestion().getCategory().getName(),
                        a.getSelectedOption(),
                        a.getQuestion().getCorrectOption(),
                        a.isCorrect(),
                        a.getTimeSpentMs()))
                .sorted(Comparator.comparingInt(AnswerDetailDto::number))
                .toList();

        AnswerDetailDto slowest = details.stream()
                .max(Comparator.comparingLong(AnswerDetailDto::timeSpentMs))
                .orElse(null);

        Map<Long, CategoryAccumulator> byCategory = new LinkedHashMap<>();
        for (AnswerDetailDto d : details) {
            byCategory.computeIfAbsent(d.categoryId(), id -> new CategoryAccumulator(d.categoryName()))
                    .add(d.timeSpentMs());
        }
        List<CategoryTimeBreakdownDto> breakdown = byCategory.entrySet().stream()
                .map(e -> new CategoryTimeBreakdownDto(e.getKey(), e.getValue().name, e.getValue().totalMs, e.getValue().count))
                .toList();

        return new AdminAttemptDetailDto(toSummary(attempt), details, slowest, breakdown);
    }

    private AdminAttemptSummaryDto toSummary(Attempt attempt) {
        return new AdminAttemptSummaryDto(
                attempt.getId(),
                attempt.getFirstName(),
                attempt.getLastName(),
                attempt.getTeam(),
                attempt.getIpAddress(),
                attempt.getUserAgent(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getStatus().name(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                attempt.getScorePercent(),
                attempt.isTimingSuspicious());
    }

    private static final class CategoryAccumulator {
        final String name;
        long totalMs = 0;
        int count = 0;

        CategoryAccumulator(String name) {
            this.name = name;
        }

        void add(long ms) {
            totalMs += ms;
            count++;
        }
    }
}
